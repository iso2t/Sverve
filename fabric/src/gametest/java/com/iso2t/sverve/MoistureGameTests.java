package com.iso2t.sverve;

import com.iso2t.sverve.network.moisture.MoistureSyncPayload;
import com.iso2t.sverve.network.moisture.MoistureSyncTracker;
import com.iso2t.sverve.network.moisture.MoistureSyncTransport;
import com.iso2t.sverve.network.moisture.MoistureSynchronizer;
import com.iso2t.sverve.player.environment.BiomeEnvironmentSampler;
import com.iso2t.sverve.survival.moisture.MoistureState;
import com.iso2t.sverve.survival.temperature.BiomeTemperatureMapping;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Native water/rain/shelter, feature independence, cooling, and saved attachment lifecycle.
 */
public final class MoistureGameTests {
	@GameTest
	public void hudSnapshotsBelongToTheirOwnerAndOnlyVisibleChangesOrRefreshesSendPackets (GameTestHelper helper) {
		var first = join(helper);
		var second = join(helper);
		var transport = new RecordingTransport();
		var sync = new MoistureSynchronizer(runtime().getPlayerMoisture(), runtime().getConfig().getMoisture(), transport);
		try {
			seed(first, 0.5);
			seed(second, 1);
			sync.refresh(first);
			sync.refresh(second);
			helper.assertTrue(transport.sent.size() == 2, "Each owner must receive an initial snapshot");
			helper.assertTrue(transport.sent.getFirst().owner() == first && transport.sent.getFirst().payload().getSnapshot().getFillUnits() == 4, "First owner must receive their fill");
			helper.assertTrue(transport.sent.getLast().owner() == second && transport.sent.getLast().payload().getSnapshot().getFillUnits() == 7, "Second owner must receive their fill");
			seed(first, 0.49);
			sync.update(first);
			helper.assertTrue(transport.sent.size() == 2, "Unchanged visible fill must not send packets");
			seed(first, 0.4);
			sync.update(first);
			helper.assertTrue(transport.sent.size() == 3 && transport.sent.getLast().owner() == first && transport.sent.getLast().payload().getSnapshot().getFillUnits() == 3, "Drying changes go only to their owner");
			sync.refresh(first);
			helper.assertTrue(transport.sent.size() == 4, "Lifecycle transitions must refresh even unchanged fill");
			seed(first, 0);
			sync.update(first);
			helper.assertTrue(transport.sent.getLast().payload().getSnapshot().getFillUnits() == 0, "Drying fully must hide the icon");
		} finally {
			leave(first);
			leave(second);
		}
		helper.succeed();
	}

	@GameTest
	public void hudDeliveryRetriesAndEnableChangesReachPausedPlayers (GameTestHelper helper) {
		var player = join(helper);
		var config = runtime().getConfig().getMoisture();
		var transport = new RecordingTransport();
		var sync = new MoistureSynchronizer(runtime().getPlayerMoisture(), config, transport);
		try {
			seed(player, 0.5);
			transport.ready = false;
			sync.refresh(player);
			helper.assertTrue(transport.sent.isEmpty(), "An unavailable channel cannot acknowledge delivery");
			transport.ready = true;
			sync.update(player);
			helper.assertTrue(transport.sent.size() == 1, "Next tick must retry the snapshot");
			player.setGameMode(GameType.CREATIVE);
			config.getEnabled().set(false);
			sync.update(player);
			helper.assertTrue(transport.sent.size() == 2 && !transport.sent.getLast().payload().getSnapshot().isEnabled(), "Disabling must reach the owner while gameplay is paused");
			sync.update(player);
			helper.assertTrue(transport.sent.size() == 2, "Unchanged disabled state must deduplicate");
			config.getEnabled().set(true);
			sync.update(player);
			helper.assertTrue(transport.sent.size() == 3 && transport.sent.getLast().payload().getSnapshot().isEnabled(), "Re-enabling must restore the owner snapshot");
		} finally {
			config.getEnabled().set(true);
			leave(player);
		}
		helper.succeed();
	}

	private record Delivery(ServerPlayer owner, MoistureSyncPayload payload) {
	}

	private static final class RecordingTransport implements MoistureSyncTransport {
		private final Map<ServerPlayer, MoistureSyncTracker> trackers = new HashMap<>();
		private final List<Delivery>                         sent     = new ArrayList<>();
		private       boolean                                ready    = true;

		@Override
		public MoistureSyncTracker tracker (ServerPlayer player) {
			return trackers.computeIfAbsent(player, ignored -> new MoistureSyncTracker());
		}

		@Override
		public boolean send (ServerPlayer player, MoistureSyncPayload payload) {
			if (!ready) return false;
			sent.add(new Delivery(player, payload));
			return true;
		}
	}

	@GameTest
	public void waterSoaksThenDryingAndCoolingUseTheUpdatedWetnessWithoutRefillingThirst (GameTestHelper helper) {
		var player = join(helper);
		var pos = player.blockPosition();
		try {
			biome(helper, player, "minecraft:plains");
			player.level().setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
			player.level().setBlock(pos.above(), Blocks.WATER.defaultBlockState(), 3);
			player.baseTick();
			helper.assertTrue(sampler().sample(player).isImmersed(), "Actual vanilla water contact must be sampled");
			double hydration = runtime().getPlayerThirst().get(player).getHydration();
			ticks(helper, 19);
			near(helper, wetness(player), 0);
			ticks(helper, 1);
			near(helper, wetness(player), 1);
			near(helper, exposure(player), -0.35 * -Math.expm1(-0.02));
			helper.assertTrue(runtime().getPlayerThirst().get(player).getHydration() < hydration, "Immersion must not refill thirst");
			player.setPos(player.position().add(5, 0, 0));
			player.baseTick();
			helper.assertTrue(!sampler().sample(player).isImmersed(), "Leaving water must stop immersion");
			double before = exposure(player);
			ticks(helper, 20);
			near(helper, wetness(player), 0.995);
			helper.assertTrue(exposure(player) < before, "Persisting wetness must keep cooling the body while drying");
		} finally {
			player.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			player.level().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void rainWetsExposedPlayersWhileRoofsSnowAndRainlessBiomesDoNot (GameTestHelper helper) {
		var player = join(helper);
		var level = player.level();
		float originalRain = level.getRainLevel(1);
		var roof = player.blockPosition().above(3);
		try {
			biome(helper, player, "minecraft:plains");
			level.setRainLevel(1);
			helper.assertTrue(sampler().sample(player).isExposedToRain(), "Exposed rainy plains must wet players");
			ticks(helper, 20);
			near(helper, wetness(player), 0.05);
			level.setBlock(roof, Blocks.STONE.defaultBlockState(), 3);
			helper.assertTrue(!sampler().sample(player).isExposedToRain(), "A real roof must block rain exposure");
			ticks(helper, 20);
			near(helper, wetness(player), 0.045);
			level.setBlock(roof, Blocks.AIR.defaultBlockState(), 3);
			biome(helper, player, "minecraft:desert");
			helper.assertTrue(!sampler().sample(player).isExposedToRain(), "Rainless biomes must not wet players");
			biome(helper, player, "minecraft:snowy_plains");
			helper.assertTrue(!sampler().sample(player).isExposedToRain(), "Snow must not count as liquid rain");
		} finally {
			level.setBlock(roof, Blocks.AIR.defaultBlockState(), 3);
			level.setRainLevel(originalRain);
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void hotBiomesDryFasterAndWetBodiesStayCoolerThanDryBodies (GameTestHelper helper) {
		var wet = join(helper);
		var dry = join(helper);
		try {
			biome(helper, wet, "sverve_test:hot_test");
			seed(wet, 1);
			ticks(helper, 20);
			near(helper, wetness(wet), 0.985);
			near(helper, wetness(dry), 0);
			helper.assertTrue(exposure(wet) < exposure(dry), "Wet players in the same hot biome must warm more slowly");
			seed(wet, 0.002);
			ticks(helper, 20);
			near(helper, wetness(wet), 0);
		} finally {
			leave(wet);
			leave(dry);
		}
		helper.succeed();
	}

	@GameTest
	public void moistureAndTemperatureSwitchesPauseIndependentlyAndExemptModesDiscardPartialTime (GameTestHelper helper) {
		var player = join(helper);
		var moisture = runtime().getConfig().getMoisture();
		var temperature = runtime().getConfig().getTemperature();
		try {
			biome(helper, player, "minecraft:plains");
			seed(player, 1);
			temperature.getEnabled().set(false);
			ticks(helper, 10);
			near(helper, wetness(player), 1);
			temperature.getEnabled().set(true);
			ticks(helper, 10);
			near(helper, wetness(player), 0.995);
			near(helper, exposure(player), 0);
			ticks(helper, 10);
			helper.assertTrue(exposure(player) < 0, "Temperature must wait its own fresh interval before wet cooling");
			moisture.getEnabled().set(false);
			runtime().getPlayerTemperature().update(player, ignored -> TemperatureState.comfortable());
			ticks(helper, 20);
			near(helper, wetness(player), 0.995);
			near(helper, exposure(player), 0);
			moisture.getEnabled().set(true);
			ticks(helper, 19);
			for (var mode : new GameType[] { GameType.CREATIVE, GameType.SPECTATOR }) {
				player.setGameMode(mode);
				ticks(helper, 100);
				near(helper, wetness(player), 0.995);
			}
			player.setGameMode(GameType.ADVENTURE);
			ticks(helper, 19);
			near(helper, wetness(player), 0.995);
			ticks(helper, 1);
			near(helper, wetness(player), 0.99);
			player.setHealth(0);
			ticks(helper, 100);
			near(helper, wetness(player), 0.99);
		} finally {
			moisture.getEnabled().set(true);
			temperature.getEnabled().set(true);
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void wetnessSurvivesNativeSaveReloadAndDimensionTransferAndDeathRespawnsDry (GameTestHelper helper) {
		var player = join(helper);
		var server = helper.getLevel().getServer();
		var rules = helper.getLevel().getGameRules();
		boolean keepInventory = rules.get(GameRules.KEEP_INVENTORY);
		try {
			seed(player, 0.625);
			ticks(helper, 19);
			var profile = player.getGameProfile();
			leave(player);
			player = join(helper, profile);
			near(helper, wetness(player), 0.625);
			ticks(helper, 19);
			near(helper, wetness(player), 0.625);
			ticks(helper, 1);
			helper.assertTrue(wetness(player) < 0.625, "Rejoined players must resume drying from their saved state");
			seed(player, 0.5);
			player.teleportTo(server.getLevel(Level.NETHER), 0.5, 80, 0.5, Set.of(), 0, 0, true);
			near(helper, wetness(player), 0.5);
			player.teleportTo(helper.getLevel(), 0.5, 200, 0.5, Set.of(), 0, 0, true);
			for (boolean enabled : new boolean[] { false, true }) {
				rules.set(GameRules.KEEP_INVENTORY, enabled, server);
				seed(player, 0.8);
				player.setHealth(0);
				player = server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
				player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
				near(helper, wetness(player), 0);
			}
		} finally {
			rules.set(GameRules.KEEP_INVENTORY, keepInventory, server);
			leave(player);
		}
		helper.succeed();
	}

	private static SverveRuntime runtime () {
		return FabricLoader.getInstance().getEntrypoints("main", ModInitializer.class).stream().filter(SverveFabric.class::isInstance).map(SverveFabric.class::cast).findFirst().orElseThrow().getRuntime();
	}

	private static BiomeEnvironmentSampler sampler () {
		return new BiomeEnvironmentSampler(new BiomeTemperatureMapping(runtime().getConfig().getTemperature()));
	}

	private static ServerPlayer join (GameTestHelper helper) {
		return join(helper, new GameProfile(UUID.randomUUID(), "moisture-test"));
	}

	private static ServerPlayer join (GameTestHelper helper, GameProfile profile) {
		var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, ClientInformation.createDefault());
		helper.getLevel().getServer().getPlayerList().loadPlayerData(player.nameAndId()).ifPresent(tag -> player.load(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), tag)));
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
		player.setGameMode(GameType.SURVIVAL);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		player.setPermanentlyInvulnerable(true);
		player.setNoGravity(true);
		player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 30, 0))));
		return player;
	}

	private static void biome (GameTestHelper helper, ServerPlayer player, String id) {
		var pos = player.blockPosition();
		var level = player.level();
		for (int x = (pos.getX() - 8) >> 4; x <= (pos.getX() + 8) >> 4; x++) {
			for (int z = (pos.getZ() - 8) >> 4; z <= (pos.getZ() + 8) >> 4; z++) level.getChunk(x, z);
		}
		var biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.parse(id)));
		helper.assertTrue(FillBiomeCommand.fill(level, pos.offset(-8, -8, -8), pos.offset(8, 8, 8), biome).right().isEmpty(), "Test biome fill must succeed");
	}

	private static void ticks (GameTestHelper helper, int count) {
		for (int tick = 0; tick < count; tick++) ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(helper.getLevel().getServer());
	}

	private static void seed (ServerPlayer player, double wetness) {
		runtime().getPlayerMoisture().update(player, ignored -> new MoistureState(wetness));
	}

	private static double wetness (ServerPlayer player) {
		return runtime().getPlayerMoisture().get(player).getWetness();
	}

	private static double exposure (ServerPlayer player) {
		return runtime().getPlayerTemperature().get(player).getExposure();
	}

	private static void near (GameTestHelper helper, double actual, double expected) {
		helper.assertTrue(Math.abs(actual - expected) < 1e-8, "Expected " + expected + ", got " + actual);
	}

	private static void leave (ServerPlayer player) {
		player.level().getServer().getPlayerList().remove(player);
	}
}
