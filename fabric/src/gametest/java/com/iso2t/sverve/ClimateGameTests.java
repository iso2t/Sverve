package com.iso2t.sverve;

import com.iso2t.sverve.player.environment.BiomeEnvironmentSampler;
import com.iso2t.sverve.player.environment.NearbyHeatSampler;
import com.iso2t.sverve.survival.moisture.MoistureState;
import com.iso2t.sverve.survival.temperature.BiomeTemperatureMapping;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import com.iso2t.sverve.survival.thirst.ThirstState;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.api.ModInitializer;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Registered datapack climates exercise the native sampler and shared gameplay calculation.
 */
public final class ClimateGameTests {
	@GameTest
	public void humidityChangesThirstAndDryingInCustomBiomes (GameTestHelper helper) {
		var player = join(helper);
		var runtime = runtime();
		var config = runtime.getConfig();
		var sampler = new BiomeEnvironmentSampler(new BiomeTemperatureMapping(config.getTemperature()), new NearbyHeatSampler(config.getHeatSources()));
		try {
			for (double humidity : new double[] { 0, 1 }) {
				biome(helper, player, humidity == 0 ? "sverve_test:dry_test" : "sverve_test:humid_test");
				near(helper, sampler.humidity(player), humidity);
				near(helper, sampler.sample(player).getHumidity(), humidity);
				runtime.getPlayerTemperature().update(player, ignored -> TemperatureState.comfortable());
				runtime.getPlayerThirst().update(player, ignored -> ThirstState.hydrated());
				runtime.getPlayerMoisture().update(player, ignored -> new MoistureState(1));
				for (int tick = 0; tick < 20; tick++) {
					runtime.getThirstGameplay().tick(player);
					runtime.getTemperatureGameplay().tick(player);
				}
				near(helper, runtime.getPlayerThirst().get(player).getHydration(), 1 - config.getThirst().getBaseLoss().get() - (1 - humidity) * config.getThirst().getDryAirLoss().get());
				near(helper, runtime.getPlayerMoisture().get(player).getWetness(), 1 - config.getMoisture().getDryingRate().get() * (1 - 0.75 * humidity));
			}
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void dryAirWorksWithOtherFeaturesDisabledAndSettingsApplyLive (GameTestHelper helper) {
		var player = join(helper);
		var runtime = runtime();
		var config = runtime.getConfig();
		boolean temperature = config.getTemperature().getEnabled().get();
		boolean moisture = config.getMoisture().getEnabled().get();
		double dryAirLoss = config.getThirst().getDryAirLoss().get();
		try {
			biome(helper, player, "sverve_test:dry_test");
			config.getTemperature().getEnabled().set(false);
			config.getMoisture().getEnabled().set(false);
			runtime.getPlayerTemperature().update(player, ignored -> new TemperatureState(1));
			for (double dryLoss : new double[] { 0.1, 0 }) {
				config.getThirst().getDryAirLoss().set(dryLoss);
				runtime.getPlayerThirst().update(player, ignored -> ThirstState.hydrated());
				for (int tick = 0; tick < 20; tick++) runtime.getThirstGameplay().tick(player);
				near(helper, runtime.getPlayerThirst().get(player).getHydration(), 1 - config.getThirst().getBaseLoss().get() - dryLoss);
			}
		} finally {
			config.getTemperature().getEnabled().set(temperature);
			config.getMoisture().getEnabled().set(moisture);
			config.getThirst().getDryAirLoss().set(dryAirLoss);
			leave(player);
		}
		helper.succeed();
	}

	private static SverveRuntime runtime () {
		return FabricLoader.getInstance().getEntrypoints("main", ModInitializer.class).stream().filter(SverveFabric.class::isInstance).map(SverveFabric.class::cast).findFirst().orElseThrow().getRuntime();
	}

	private static ServerPlayer join (GameTestHelper helper) {
		var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
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
		helper.assertTrue(FillBiomeCommand.fill(level, pos.offset(-8, -8, -8), pos.offset(8, 8, 8), biome).right().isEmpty(), "Biome fill must succeed");
	}

	private static void near (GameTestHelper helper, double actual, double expected) {
		helper.assertTrue(Math.abs(actual - expected) < 1e-8, "Expected " + expected + ", got " + actual);
	}

	private static void leave (ServerPlayer player) {
		player.level().getServer().getPlayerList().remove(player);
	}
}
