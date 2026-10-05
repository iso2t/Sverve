package com.iso2t.sverve.player.exertion;

import com.iso2t.sverve.player.SurvivalEligibility;
import com.iso2t.sverve.survival.exertion.LoadThirstCost;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

@RequiredArgsConstructor
public final class MovementThirst {

	@NonNull
	private final LoadThirstCost system;

	public double waterLoss (ServerPlayer player, double dx, double dy, double dz) {
		if (isExempt(player)) return 0.0;
		var ratio = CarriedLoad.ratio(player);
		if (ratio == 0.0) return 0.0;
		var submerged = player.isSwimming() || player.isEyeInFluid(FluidTags.WATER);
		var climbing = player.onClimbable();
		if (!player.onGround() && !player.isInWater() && !submerged && !climbing) return 0.0;
		if ((player.isInWater() || submerged) && inCurrent(player)) return 0.0;
		var intent = movementIntent(player, submerged, climbing, dy);
		var movement = new Vec3(dx, submerged || climbing ? dy : 0.0, dz);
		return system.waterLoss(ratio, voluntaryDistance(movement, intent), player.isSprinting());
	}

	private static boolean isExempt (ServerPlayer player) {
		return !SurvivalEligibility.canUpdate(player.gameMode(), player.isAlive()) || player.isPassenger() || player.isFallFlying() || player.getAbilities().flying;
	}

	private static Vec3 movementIntent (ServerPlayer player, boolean submerged, boolean climbing, double dy) {
		var input = player.getLastClientInput();
		var intent = player.getLastClientMoveIntent();
		if (submerged) {
			return swimmingIntent(player, intent);
		}
		return climbing && dy > 0.0 && (input.forward() || input.jump()) ? intent.add(0.0, 1.0, 0.0) : intent;
	}

	private static Vec3 swimmingIntent (ServerPlayer player, Vec3 intent) {
		var input = player.getLastClientInput();
		if (player.isSwimming() && input.forward() != input.backward()) {
			var forward = input.forward() ? 1.0 : -1.0;
			if (input.left() != input.right()) forward /= Math.sqrt(2.0);
			var horizontalLook = Vec3.directionFromRotation(0.0F, player.getYRot());
			intent = intent.add(player.getLookAngle().subtract(horizontalLook).scale(forward));
		}
		var vertical = 0.0;
		if (input.jump() != input.shift()) vertical = input.jump() ? 1.0 : -1.0;
		return intent.add(0.0, vertical, 0.0);
	}

	public static double voluntaryDistance (@NonNull Vec3 movement, @NonNull Vec3 intent) {
		if (!movement.isFinite() || !intent.isFinite() || intent.lengthSqr() < 1.0E-8) return 0.0;
		return Math.clamp(movement.dot(intent.normalize()), 0.0, movement.length());
	}

	private static boolean inCurrent (ServerPlayer player) {
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
