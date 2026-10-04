package com.iso2t.sverve.player.exertion;

import com.iso2t.sverve.player.SurvivalEligibility;
import com.iso2t.sverve.survival.exertion.LoadExertionSystem;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Bridges vanilla's movement statistics to a bounded, voluntary load cost.
 */
@RequiredArgsConstructor
public final class LoadExertion {
	@NonNull
	private final LoadExertionSystem system;

	public double waterLoss (ServerPlayer player, double dx, double dy, double dz) {
		if (!SurvivalEligibility.canUpdate(player.gameMode(), player.isAlive()) || player.isPassenger() || player.isFallFlying() || player.getAbilities().flying) return 0.0;
		double ratio = CarriedLoad.ratio(player);
		if (ratio == 0.0) return 0.0;
		boolean submerged = player.isSwimming() || player.isEyeInFluid(FluidTags.WATER);
		boolean climbing = player.onClimbable();
		if (!player.onGround() && !player.isInWater() && !submerged && !climbing) return 0.0;
		if ((player.isInWater() || submerged) && hasExternalFluidMotion(player)) return 0.0;
		var input = player.getLastClientInput();
		var intent = player.getLastClientMoveIntent();
		if (submerged) {
			if (player.isSwimming() && input.forward() != input.backward()) {
				double forward = (input.forward() ? 1.0 : -1.0) / (input.left() != input.right() ? Math.sqrt(2.0) : 1.0);
				intent = intent.add(player.getLookAngle().subtract(Vec3.directionFromRotation(0.0F, player.getYRot())).scale(forward));
			}
			intent = intent.add(0.0, input.jump() == input.shift() ? 0.0 : input.jump() ? 1.0 : -1.0, 0.0);
		} else if (climbing && dy > 0.0 && (input.forward() || input.jump())) {
			intent = intent.add(0.0, 1.0, 0.0);
		}
		var movement = new Vec3(dx, submerged || climbing ? dy : 0.0, dz);
		return system.waterLoss(ratio, voluntaryDistance(movement, intent), player.isSprinting());
	}

	public static double voluntaryDistance (@NonNull Vec3 movement, @NonNull Vec3 intent) {
		if (!movement.isFinite() || !intent.isFinite() || intent.lengthSqr() < 1.0E-8) return 0.0;
		return Math.clamp(movement.dot(intent.normalize()), 0.0, movement.length());
	}

	private static boolean hasExternalFluidMotion (ServerPlayer player) {
		var box = player.getBoundingBox().deflate(0.001);
		for (var pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ), Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
			var state = player.level().getBlockState(pos);
			if (state.is(Blocks.BUBBLE_COLUMN)) return true;
			var fluid = state.getFluidState();
			if (!fluid.isEmpty() && fluid.getFlow(player.level(), pos).lengthSqr() > 1.0E-8) return true;
		}
		return false;
	}
}
