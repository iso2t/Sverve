package com.iso2t.sverve.survival.exertion;

import com.iso2t.sverve.survival.SurvivalMath;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Pure load/distance calculation; standing still and involuntary drift add no water cost.
 */
@RequiredArgsConstructor
public final class LoadExertionSystem {
	@NonNull
	private final LoadExertionConfig config;

	public double waterLoss (double loadRatio, double distance, boolean sprinting) {
		SurvivalMath.requireRange(loadRatio, 0.0, Double.MAX_VALUE, "loadRatio");
		SurvivalMath.requireRange(distance, 0.0, Double.MAX_VALUE, "distance");
		if (!config.getEnabled().get() || distance == 0.0) return 0.0;
		double free = config.getFreeLoadRatio().get();
		double effort = Math.max(0.0, (Math.min(loadRatio, config.getMaximumLoadRatio().get()) - free) / (1.0 - free));
		return distance * config.getWaterLossPerBlock().get() * effort * (sprinting ? config.getSprintMultiplier().get() : 1.0);
	}
}
