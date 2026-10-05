package com.iso2t.sverve.survival.exertion;

import com.iso2t.sverve.config.LoadThirstConfig;
import com.iso2t.sverve.survival.Mth;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class LoadThirstCost {

	@NonNull
	private final LoadThirstConfig config;

	public double waterLoss (double loadRatio, double distance, boolean sprinting) {
		Mth.requireRange(loadRatio, 0.0, Double.MAX_VALUE, "loadRatio");
		Mth.requireRange(distance, 0.0, Double.MAX_VALUE, "distance");
		if (!config.getEnabled().get() || distance == 0.0) return 0.0;
		var free = config.getFreeLoadRatio().get();
		var effort = Math.max(0.0, (Math.min(loadRatio, config.getMaximumLoadRatio().get()) - free) / (1.0 - free));
		return distance * config.getWaterLossPerBlock().get() * effort * (sprinting ? config.getSprintMultiplier().get() : 1.0);
	}
}
