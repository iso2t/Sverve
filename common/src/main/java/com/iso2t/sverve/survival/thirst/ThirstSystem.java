package com.iso2t.sverve.survival.thirst;

import com.iso2t.sverve.survival.SurvivalMath;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ThirstSystem {
	@NonNull
	private final ThirstConfig config;

	/**
	 * One additive loss model shared by native gameplay and the aggregate simulation.
	 */
	public ThirstState advance (@NonNull ThirstState state, boolean sprinting, double humidity, double bodyHeat, double seconds) {
		SurvivalMath.requireRange(bodyHeat, 0.0, 1.0, "bodyHeat");
		SurvivalMath.requireRange(humidity, 0.0, 1.0, "humidity");
		double loss = config.getBaseLoss().get() + bodyHeat * config.getHeatLoss().get() + (sprinting ? config.getSprintLoss().get() : 0.0) + (1.0 - humidity) * config.getDryAirLoss().get();
		return loseHydration(state, loss, seconds);
	}

	public ThirstState drinkWater (@NonNull ThirstState state) {
		return config.getEnabled().get() ? state.drink(config.getWaterBottleHydration().get()) : state;
	}

	private ThirstState loseHydration (ThirstState state, double loss, double seconds) {
		SurvivalMath.requireRange(seconds, 0.0, Double.MAX_VALUE, "seconds");
		if (!config.getEnabled().get() || seconds == 0.0 || loss == 0.0 || state.getHydration() == 0.0) return state;
		return new ThirstState(SurvivalMath.clamp(state.getHydration() - loss * seconds, 0.0, 1.0));
	}
}
