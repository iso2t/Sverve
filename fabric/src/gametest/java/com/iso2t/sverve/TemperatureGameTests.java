package com.iso2t.sverve;

import com.iso2t.sverve.network.temperature.TemperatureSyncPayload;
import com.iso2t.sverve.network.temperature.TemperatureSyncTracker;
import com.iso2t.sverve.network.temperature.TemperatureSyncTransport;
import com.iso2t.sverve.network.temperature.TemperatureSynchronizer;
import com.iso2t.sverve.player.environment.BiomeEnvironmentSampler;
import com.iso2t.sverve.survival.temperature.BiomeTemperatureMapping;
import com.iso2t.sverve.survival.temperature.TemperatureBand;
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
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Exercises current-biome lookup, native server tick wiring, and native attachment copying.
 */
public final class TemperatureGameTests {
	@GameTest
	public void ownerSnapshotsDeduplicateHoldBoundaryBufferAndRefreshLifecycle (GameTestHelper helper) {
		var first = join(helper);
		var second = join(helper);
		var transport = new RecordingTransport();
		var sync = new TemperatureSynchronizer(runtime().getPlayerTemperature(), runtime().getConfig().getTemperature(), transport);
		try {
			seed(first, 0);
			seed(second, -0.5);
			sync.refresh(first);
			sync.refresh(second);
			helper.assertTrue(transport.sent.size() == 2, "Each owner must receive their own first snapshot");
			helper.assertTrue(transport.sent.get(0).owner() == first && transport.sent.get(0).payload().getSnapshot().getBand() == TemperatureBand.NORMAL, "The first owner must receive their own comfortable band");
			helper.assertTrue(transport.sent.get(1).owner() == second && transport.sent.get(1).payload().getSnapshot().getBand() == TemperatureBand.COLD, "The second owner must receive their own cold band");
			seed(first, 0.26);
			sync.update(first);
			helper.assertTrue(transport.sent.size() == 2, "Boundary noise must not send another packet");
			seed(first, 0.30);
			sync.update(first);
			helper.assertTrue(transport.sent.size() == 3 && transport.sent.getLast().payload().getSnapshot().getBand() == TemperatureBand.WARM, "Crossing the buffer must send a warm band only to its owner");
			seed(first, 0.24);
			sync.update(first);
			helper.assertTrue(transport.sent.size() == 3, "Returning into the buffer must keep the warm band");
			sync.refresh(first);
			helper.assertTrue(transport.sent.size() == 4 && transport.sent.getLast().payload().getSnapshot().getBand() == TemperatureBand.NORMAL, "Lifecycle refresh must classify current exposure without an old band latch");
		} finally {
			leave(first);
			leave(second);
		}
		helper.succeed();
	}

	@GameTest
	public void unavailableChannelsRetryAndServerEnableChangesReachExemptPlayers (GameTestHelper helper) {
		var player = join(helper);
		var config = runtime().getConfig().getTemperature();
		var transport = new RecordingTransport();
		var sync = new TemperatureSynchronizer(runtime().getPlayerTemperature(), config, transport);
		try {
			seed(player, 0.5);
			transport.ready = false;
			sync.refresh(player);
			helper.assertTrue(transport.sent.isEmpty(), "Unavailable channels must not count as delivery");
			transport.ready = true;
			sync.update(player);
			helper.assertTrue(transport.sent.size() == 1, "The next update must retry the initial snapshot");
			player.setGameMode(GameType.CREATIVE);
			config.getEnabled().set(false);
			sync.update(player);
			helper.assertTrue(transport.sent.size() == 2 && !transport.sent.getLast().payload().getSnapshot().isEnabled(), "Config disabling must reach exempt owners even while simulation is paused");
			sync.update(player);
			helper.assertTrue(transport.sent.size() == 2, "Unchanged disabled snapshots must deduplicate");
			config.getEnabled().set(true);
			sync.update(player);
			helper.assertTrue(transport.sent.size() == 3 && transport.sent.getLast().payload().getSnapshot().isEnabled(), "Re-enabling must reach the same owner");
		} finally {
			config.getEnabled().set(true);
			leave(player);
		}
		helper.succeed();
	}

	private record Delivery(ServerPlayer owner, TemperatureSyncPayload payload) {
	}

	private static final class RecordingTransport implements TemperatureSyncTransport {
		private final Map<ServerPlayer, TemperatureSyncTracker> trackers = new HashMap<>();
		private final List<Delivery>                            sent     = new ArrayList<>();
		private       boolean                                   ready    = true;

		@Override
		public TemperatureSyncTracker tracker (ServerPlayer player) {
			return trackers.computeIfAbsent(player, ignored -> new TemperatureSyncTracker());
		}

		@Override
		public boolean send (ServerPlayer player, TemperatureSyncPayload payload) {
			if (!ready) return false;
			sent.add(new Delivery(player, payload));
			return true;
		}
	}

	@GameTest
	public void customRegisteredBiomeWarmsAndSwitchingToSnowyPlainsCools (GameTestHelper helper) {
		var player = join(helper);
		try {
			near(helper, exposure(player), 0);
			biome(helper, player, "sverve_test:hot_test");
			var sampler = new BiomeEnvironmentSampler(new BiomeTemperatureMapping(runtime().getConfig().getTemperature()));
			near(helper, sampler.sample(player).getAmbientTemperature(), 1);
			ticks(helper, 19);
			near(helper, exposure(player), 0);
			ticks(helper, 1);
			double first = -Math.expm1(-0.02);
			near(helper, exposure(player), first);
			biome(helper, player, "minecraft:snowy_plains");
			near(helper, sampler.sample(player).getAmbientTemperature(), -1);
			ticks(helper, 20);
			near(helper, exposure(player), first + (-1 - first) * -Math.expm1(-0.02));
			helper.assertTrue(exposure(player) < first, "The next sample must use the player's new biome");
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void settingsApplyLiveAndExemptOrDisabledTicksDoNotAccumulate (GameTestHelper helper) {
		var player = join(helper);
		var config = runtime().getConfig().getTemperature();
		try {
			biome(helper, player, "sverve_test:hot_test");
			seed(player, 0.4);
			ticks(helper, 19);
			for (var mode : new GameType[] { GameType.CREATIVE, GameType.SPECTATOR }) {
				player.setGameMode(mode);
				ticks(helper, 100);
				near(helper, exposure(player), 0.4);
			}
			player.setGameMode(GameType.ADVENTURE);
			config.getEnabled().set(false);
			ticks(helper, 200);
			near(helper, exposure(player), 0.4);
			config.getEnabled().set(true);
			config.getResponseRate().set(1.0);
			ticks(helper, 19);
			near(helper, exposure(player), 0.4);
			ticks(helper, 1);
			near(helper, exposure(player), 0.4 + 0.6 * -Math.expm1(-1));
			config.getComfortableBiomeTemperature().set(4.0);
			config.getColdBiomeRange().set(0.8);
			ticks(helper, 20);
			helper.assertTrue(exposure(player) < 0, "Live mapping changes must alter the target temperature");
			double beforeDeath = exposure(player);
			player.setHealth(0);
			ticks(helper, 100);
			near(helper, exposure(player), beforeDeath);
		} finally {
			config.getEnabled().set(true);
			config.getResponseRate().set(0.02);
			config.getComfortableBiomeTemperature().set(0.8);
			config.getColdBiomeRange().set(0.8);
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void playersKeepIndependentTemperatureAndOnlyWarmPlayersLoseThirstFaster (GameTestHelper helper) {
		var first = join(helper);
		var second = join(helper);
		try {
			seed(first, 0.7);
			seed(second, -0.6);
			biome(helper, first, "sverve_test:hot_test");
			double firstThirst = runtime().getPlayerThirst().get(first).getHydration();
			double secondThirst = runtime().getPlayerThirst().get(second).getHydration();
			ticks(helper, 20);
			helper.assertTrue(exposure(first) > 0.7, "Hot players must continue warming");
			helper.assertTrue(exposure(second) > -0.6, "The second player's state must advance independently");
			near(helper, runtime().getPlayerThirst().get(first).getHydration(), firstThirst - 1.5 / 1200);
			near(helper, runtime().getPlayerThirst().get(second).getHydration(), secondThirst - 1.0 / 1200);
		} finally {
			leave(first);
			leave(second);
		}
		helper.succeed();
	}

	@GameTest
	public void nativePlayerDataReloadRestoresTemperatureAndStartsAFreshSamplingInterval (GameTestHelper helper) {
		var player = join(helper);
		try {
			seed(player, -0.375);
			ticks(helper, 19);
			var profile = player.getGameProfile();
			leave(player); // PlayerList saves native attachments before removing this player.
			player = join(helper, profile);
			near(helper, exposure(player), -0.375);
			biome(helper, player, "sverve_test:hot_test");
			ticks(helper, 19);
			near(helper, exposure(player), -0.375);
			ticks(helper, 1);
			helper.assertTrue(exposure(player) > -0.375, "Rejoined players must resume from saved temperature");
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void dimensionTransferPreservesBodyTemperatureAndDeathResetsWithEitherKeepInventorySetting (GameTestHelper helper) {
		var player = join(helper);
		var rules = helper.getLevel().getGameRules();
		boolean originalKeepInventory = rules.get(GameRules.KEEP_INVENTORY);
		try {
			seed(player, 0.625);
			var server = helper.getLevel().getServer();
			player.teleportTo(server.getLevel(Level.NETHER), 0.5, 80, 0.5, Set.of(), 0, 0, true);
			near(helper, exposure(player), 0.625);
			player.teleportTo(helper.getLevel(), 0.5, 100, 0.5, Set.of(), 0, 0, true);
			near(helper, exposure(player), 0.625);
			for (boolean keepInventory : new boolean[] { false, true }) {
				rules.set(GameRules.KEEP_INVENTORY, keepInventory, server);
				seed(player, -0.375);
				player.setHealth(0);
				player = server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
				player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
				near(helper, exposure(player), 0);
			}
		} finally {
			rules.set(GameRules.KEEP_INVENTORY, originalKeepInventory, helper.getLevel().getServer());
			leave(player);
		}
		helper.succeed();
	}

	private static SverveRuntime runtime () {
		return FabricLoader.getInstance().getEntrypoints("main", ModInitializer.class).stream().filter(SverveFabric.class::isInstance).map(SverveFabric.class::cast).findFirst().orElseThrow().getRuntime();
	}

	private static ServerPlayer join (GameTestHelper helper) {
		return join(helper, new GameProfile(UUID.randomUUID(), "temperature-test"));
	}

	private static ServerPlayer join (GameTestHelper helper, GameProfile profile) {
		var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, ClientInformation.createDefault());
		// In 26.3, configuration/login loads player data before PlayerList places the player.
		helper.getLevel().getServer().getPlayerList().loadPlayerData(player.nameAndId()).ifPresent(tag -> player.load(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), tag)));
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
		player.setGameMode(GameType.SURVIVAL);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 10, 0))));
		return player;
	}

	private static void biome (GameTestHelper helper, ServerPlayer player, String id) {
		var pos = player.blockPosition();
		var level = player.level();
		for (int x = (pos.getX() - 8) >> 4; x <= (pos.getX() + 8) >> 4; x++) {
			for (int z = (pos.getZ() - 8) >> 4; z <= (pos.getZ() + 8) >> 4; z++) level.getChunk(x, z);
		}
		var biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.parse(id)));
		var result = FillBiomeCommand.fill(level, pos.offset(-8, -8, -8), pos.offset(8, 8, 8), biome);
		helper.assertTrue(result.right().isEmpty(), "Test biome fill must succeed");
		helper.assertTrue(level.getBiome(pos).is(biome.unwrapKey().orElseThrow()), "The sampled biome must match the fixture");
	}

	private static void ticks (GameTestHelper helper, int count) {
		for (int tick = 0; tick < count; tick++) ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(helper.getLevel().getServer());
	}

	private static void seed (ServerPlayer player, double exposure) {
		runtime().getPlayerTemperature().update(player, state -> new TemperatureState(exposure));
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
