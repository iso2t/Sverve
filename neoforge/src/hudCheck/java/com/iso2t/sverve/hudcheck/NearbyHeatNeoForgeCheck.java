package com.iso2t.sverve.hudcheck;

import com.iso2t.sverve.player.environment.NearbyHeatSampler;
import com.iso2t.sverve.survival.environment.HeatSourceConfig;
import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Real NeoForge block/tag sampling; leaves a campfire for the native tick check.
 */
@UtilityClass
public class NearbyHeatNeoForgeCheck {
	public static BlockPos prepare (ServerPlayer player) {
		var config = new HeatSourceConfig();
		var sampler = new NearbyHeatSampler(config);
		var level = player.level();
		var torch = player.blockPosition().east();
		var fire = player.blockPosition().east(2);
		try {
			level.setBlock(torch.below(), Blocks.STONE.defaultBlockState(), 3);
			level.setBlock(torch, Blocks.TORCH.defaultBlockState(), 3);
			double mild = sampler.sample(player);
			require(mild > 0 && mild < 0.2, "NeoForge placed torches must supply mild warmth");
			level.setBlock(torch, Blocks.SOUL_TORCH.defaultBlockState(), 3);
			require(Math.abs(sampler.sample(player) - mild) < 1e-8, "Soul torches must also supply warmth");
			level.setBlock(torch, Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState(), 3);
			double strong = sampler.sample(player);
			require(strong > mild, "NeoForge campfires must supply stronger warmth");
			level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false), 3);
			require(sampler.sample(player) == 0, "Unlit campfires must supply no warmth");
			level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState(), 3);
			level.setBlock(torch, Blocks.STONE.defaultBlockState(), 3);
			level.setBlock(torch.above(), Blocks.STONE.defaultBlockState(), 3);
			require(sampler.sample(player) == 0, "NeoForge walls must block warmth");
			level.setBlock(torch, Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(torch.above(), Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(fire, Blocks.LAVA.defaultBlockState(), 3);
			double lava = sampler.sample(player);
			require(lava > strong, "NeoForge lava must supply stronger warmth than a campfire");
			level.setBlock(fire, Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 7), 3);
			require(Math.abs(sampler.sample(player) - lava) < 1e-8, "Flowing lava must also supply warmth");
			level.setBlock(torch, Blocks.STONE.defaultBlockState(), 3);
			level.setBlock(torch.above(), Blocks.STONE.defaultBlockState(), 3);
			require(sampler.sample(player) == 0, "NeoForge walls must block lava warmth");
			level.setBlock(torch, Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(torch.above(), Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState(), 3);
			config.getEnabled().set(false);
			require(sampler.sample(player) == 0, "Disabling nearby heat must remove warmth");
			return fire;
		} catch (RuntimeException | Error failure) {
			level.setBlock(fire, Blocks.AIR.defaultBlockState(), 3);
			throw failure;
		} finally {
			for (var pos : new BlockPos[] { torch, torch.above(), torch.below() }) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		}
	}

	public static void clearLava (ServerPlayer player) {
		var source = player.blockPosition().east(2);
		for (var pos : BlockPos.betweenClosed(source.offset(-6, -6, -6), source.offset(6, 6, 6))) {
			if (player.level().hasChunkAt(pos) && player.level().getFluidState(pos).is(FluidTags.LAVA)) {
				player.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			}
		}
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
