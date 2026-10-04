package com.iso2t.sverve.player.environment;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.survival.environment.HeatSourceConfig;
import com.iso2t.sverve.survival.environment.HeatSourceInfluence;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Bounded, loaded-block sampling on the existing climate cadence; never loads nearby chunks.
 */
@RequiredArgsConstructor
public final class NearbyHeatSampler {
	public static final TagKey<Block>    CAMPFIRES = heatTag("campfires");
	public static final TagKey<Block>    TORCHES   = heatTag("torches");
	@NonNull
	private final       HeatSourceConfig config;

	public double sample (@NonNull ServerPlayer player) {
		if (!config.getEnabled().get()) return 0.0;
		int radius = Math.max(config.getLavaRadius().get(), Math.max(config.getCampfireRadius().get(), config.getTorchRadius().get()));
		var center = player.blockPosition();
		var origin = player.position().add(0.0, 0.5, 0.0);
		var level = player.level();
		double strongest = 0.0;
		for (var pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
			if (!level.hasChunkAt(pos)) continue;
			double warmth = warmth(level.getBlockState(pos), origin, pos);
			if (warmth <= strongest || !hasLoadedPath(player, pos)) continue;
			var target = Vec3.atCenterOf(pos);
			var hit = level.clip(new ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)) strongest = warmth;
		}
		return strongest;
	}

	private boolean hasLoadedPath (ServerPlayer player, BlockPos target) {
		var start = player.blockPosition();
		for (int x = Math.min(start.getX(), target.getX()) >> 4; x <= Math.max(start.getX(), target.getX()) >> 4; x++) {
			for (int z = Math.min(start.getZ(), target.getZ()) >> 4; z <= Math.max(start.getZ(), target.getZ()) >> 4; z++) {
				if (!player.level().hasChunkAt(new BlockPos(x << 4, target.getY(), z << 4))) return false;
			}
		}
		return true;
	}

	private double warmth (BlockState state, Vec3 origin, BlockPos pos) {
		if (state.isAir()) return 0.0;
		int radius;
		double strength;
		if (state.getFluidState().is(FluidTags.LAVA)) {
			radius = config.getLavaRadius().get();
			strength = config.getLavaWarmth().get();
		} else {
			if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) return 0.0;
			if (state.is(CAMPFIRES)) {
				radius = config.getCampfireRadius().get();
				strength = config.getCampfireWarmth().get();
			} else if (state.is(TORCHES)) {
				radius = config.getTorchRadius().get();
				strength = config.getTorchWarmth().get();
			} else return 0.0;
		}
		// Ordinary blocks need neither a target vector nor a square root.
		if (strength == 0.0) return 0.0;
		double dx = origin.x - (pos.getX() + 0.5);
		double dy = origin.y - (pos.getY() + 0.5);
		double dz = origin.z - (pos.getZ() + 0.5);
		double distanceSquared = dx * dx + dy * dy + dz * dz;
		if (distanceSquared >= radius * radius) return 0.0;
		return HeatSourceInfluence.atDistance(Math.sqrt(distanceSquared), radius, strength);
	}

	private static TagKey<Block> heatTag (String name) {
		return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "heat_sources/" + name));
	}
}
