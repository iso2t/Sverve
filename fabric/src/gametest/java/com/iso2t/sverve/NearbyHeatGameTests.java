package com.iso2t.sverve;

import com.iso2t.sverve.player.environment.NearbyHeatSampler;
import com.iso2t.sverve.survival.environment.HeatSourceConfig;
import com.iso2t.sverve.survival.moisture.MoistureState;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Native block states, tag lookup, occlusion, and the live temperature/moisture tick bridge.
 */
public final class NearbyHeatGameTests {
	@GameTest
	public void sourceAndFlowingLavaWarmWithoutStackingAndRespectWallsAndSettings (GameTestHelper helper) {
		var player = player(helper);
		var config = new HeatSourceConfig();
		var sampler = new NearbyHeatSampler(config);
		var level = player.level();
		var source = player.blockPosition().east(2);
		var second = player.blockPosition().west(2);
		var wall = player.blockPosition().east();
		try {
			level.setBlock(source, Blocks.CAMPFIRE.defaultBlockState(), 3);
			double campfire = sampler.sample(player);
			level.setBlock(source, Blocks.LAVA.defaultBlockState(), 3);
			double lava = sampler.sample(player);
			helper.assertTrue(lava > campfire, "Nearby lava must provide stronger warmth than a campfire");
			for (int fluidLevel : new int[] { 1, 7, 8 }) {
				level.setBlock(source, Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, fluidLevel), 3);
				near(helper, sampler.sample(player), lava);
			}
			level.setBlock(second, Blocks.LAVA.defaultBlockState(), 3);
			near(helper, sampler.sample(player), lava);
			level.setBlock(second, Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
			level.setBlock(wall.above(), Blocks.STONE.defaultBlockState(), 3);
			near(helper, sampler.sample(player), 0);
			level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(wall.above(), Blocks.AIR.defaultBlockState(), 3);
			config.getLavaWarmth().set(0.5);
			near(helper, sampler.sample(player), lava / 2);
			config.getLavaRadius().set(1);
			near(helper, sampler.sample(player), 0);
			config.getLavaRadius().set(6);
			config.getEnabled().set(false);
			near(helper, sampler.sample(player), 0);
			config.getEnabled().set(true);
			player.setPos(player.position().add(9, 0, 0));
			near(helper, sampler.sample(player), 0);
			level.setBlock(source, Blocks.AIR.defaultBlockState(), 3);
			near(helper, sampler.sample(player), 0);
		} finally {
			for (var pos : new BlockPos[] { source, second, wall, wall.above() }) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		}
		helper.succeed();
	}

	@GameTest
	public void occupyingLavaSuppliesHeatButWaterDoesNot (GameTestHelper helper) {
		var player = player(helper);
		var sampler = new NearbyHeatSampler(new HeatSourceConfig());
		var pos = player.blockPosition();
		try {
			player.level().setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
			helper.assertTrue(sampler.sample(player) > 0.9, "Lava at the player's position must supply strong warmth");
			player.level().setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
			near(helper, sampler.sample(player), 0);
			player.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			near(helper, sampler.sample(player), 0);
		} finally {
			player.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		}
		helper.succeed();
	}

	@GameTest
	public void campfiresMustBeLitAndWallsBlockWarmth (GameTestHelper helper) {
		var player = player(helper);
		var config = new HeatSourceConfig();
		var sampler = new NearbyHeatSampler(config);
		var source = player.blockPosition().east(2);
		var wall = player.blockPosition().east();
		try {
			near(helper, sampler.sample(player), 0);
			player.level().setBlock(source, Blocks.CAMPFIRE.defaultBlockState(), 3);
			double campfire = sampler.sample(player);
			helper.assertTrue(campfire > 0.3, "A nearby lit campfire must supply warmth");
			player.level().setBlock(source, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false), 3);
			near(helper, sampler.sample(player), 0);
			player.level().setBlock(source, Blocks.SOUL_CAMPFIRE.defaultBlockState(), 3);
			near(helper, sampler.sample(player), campfire);
			player.level().setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
			player.level().setBlock(wall.above(), Blocks.STONE.defaultBlockState(), 3);
			near(helper, sampler.sample(player), 0);
			player.level().setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
			player.level().setBlock(wall.above(), Blocks.AIR.defaultBlockState(), 3);
			config.getEnabled().set(false);
			near(helper, sampler.sample(player), 0);
			config.getEnabled().set(true);
			config.getCampfireWarmth().set(0.4);
			near(helper, sampler.sample(player), campfire / 2);
			config.getCampfireRadius().set(1);
			near(helper, sampler.sample(player), 0);
		} finally {
			for (var pos : new BlockPos[] { source, wall, wall.above() }) player.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		}
		helper.succeed();
	}

	@GameTest
	public void torchVariantsWarmWithoutStackingAndCampfiresAreStronger (GameTestHelper helper) {
		var player = player(helper);
		var sampler = new NearbyHeatSampler(new HeatSourceConfig());
		var source = player.blockPosition().east();
		var second = player.blockPosition().west();
		try {
			player.level().setBlock(source.below(), Blocks.STONE.defaultBlockState(), 3);
			player.level().setBlock(second.below(), Blocks.STONE.defaultBlockState(), 3);
			player.level().setBlock(source, Blocks.TORCH.defaultBlockState(), 3);
			double torch = sampler.sample(player);
			helper.assertTrue(torch > 0 && torch < 0.2, "A nearby placed torch must supply mild warmth");
			player.level().setBlock(second, Blocks.TORCH.defaultBlockState(), 3);
			near(helper, sampler.sample(player), torch);
			player.level().setBlock(second, Blocks.AIR.defaultBlockState(), 3);
			player.level().setBlock(source, Blocks.SOUL_TORCH.defaultBlockState(), 3);
			near(helper, sampler.sample(player), torch);
			player.level().setBlock(source.east(), Blocks.STONE.defaultBlockState(), 3);
			for (var block : new Block[] { Blocks.WALL_TORCH, Blocks.SOUL_WALL_TORCH }) {
				player.level().setBlock(source, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 3);
				near(helper, sampler.sample(player), torch);
			}
			player.level().setBlock(source, Blocks.CAMPFIRE.defaultBlockState(), 3);
			helper.assertTrue(sampler.sample(player) > torch, "Campfires must supply more warmth than torches");
			player.setPos(player.position().add(8, 0, 0));
			near(helper, sampler.sample(player), 0);
		} finally {
			for (var pos : new BlockPos[] { source, second, source.below(), second.below(), source.east() }) player.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		}
		helper.succeed();
	}

	@GameTest
	public void nativeClimateTicksWarmAndDryNearCampfiresAndLava (GameTestHelper helper) {
		var player = player(helper);
		var server = player.level().getServer();
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
		player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 30, 0))));
		var pos = player.blockPosition();
		for (int x = (pos.getX() - 4) >> 4; x <= (pos.getX() + 4) >> 4; x++) {
			for (int z = (pos.getZ() - 4) >> 4; z <= (pos.getZ() + 4) >> 4; z++) player.level().getChunk(x, z);
		}
		var plains = player.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
		helper.assertTrue(FillBiomeCommand.fill(player.level(), pos.offset(-4, -4, -4), pos.offset(4, 4, 4), plains).right().isEmpty(), "The heat fixture must use a temperate biome");
		var source = player.blockPosition().east(2);
		try {
			for (var block : new Block[] { Blocks.CAMPFIRE, Blocks.LAVA }) {
				runtime().getPlayerTemperature().update(player, ignored -> TemperatureState.comfortable());
				runtime().getPlayerMoisture().update(player, ignored -> new MoistureState(0.8));
				player.level().setBlock(source, block.defaultBlockState(), 3);
				runtime().getTemperatureGameplay().initialize(player);
				for (int tick = 0; tick < 20; tick++) ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(server);
				helper.assertTrue(runtime().getPlayerTemperature().get(player).getExposure() > 0, "Native nearby heat must offset wet cooling in a temperate biome");
				helper.assertTrue(runtime().getPlayerMoisture().get(player).getWetness() < 0.795, "Nearby heat must dry faster than temperate air");
				player.level().setBlock(source, Blocks.AIR.defaultBlockState(), 3);
				double warm = runtime().getPlayerTemperature().get(player).getExposure();
				for (int tick = 0; tick < 20; tick++) ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(server);
				helper.assertTrue(runtime().getPlayerTemperature().get(player).getExposure() < warm, "Removing the source must remove warmth on the next sample");
			}
		} finally {
			player.level().setBlock(source, Blocks.AIR.defaultBlockState(), 3);
			server.getPlayerList().remove(player);
		}
		helper.succeed();
	}

	private static ServerPlayer player (GameTestHelper helper) {
		var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 30, 0))));
		player.setNoGravity(true);
		player.setPermanentlyInvulnerable(true);
		return player;
	}

	private static SverveRuntime runtime () {
		return FabricLoader.getInstance().getEntrypoints("main", ModInitializer.class).stream().filter(SverveFabric.class::isInstance).map(SverveFabric.class::cast).findFirst().orElseThrow().getRuntime();
	}

	private static void near (GameTestHelper helper, double actual, double expected) {
		helper.assertTrue(Math.abs(actual - expected) < 1e-8, "Expected heat " + expected + ", got " + actual);
	}
}
