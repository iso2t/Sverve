package com.iso2t.sverve.survival.thirst;

import com.iso2t.sverve.survival.SurvivalMath;
import com.iso2t.sverve.survival.environment.EnvironmentSample;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ThirstSystem {
	@NonNull
	private final ThirstConfig config;

	public ThirstState advance (@NonNull ThirstState state, @NonNull EnvironmentSample environment, double bodyHeat, double seconds) {
		SurvivalMath.requireRange(bodyHeat, 0.0, 1.0, "bodyHeat");
		double loss = config.getBaseLoss().get() + bodyHeat * config.getHeatLoss().get() + (environment.isSprinting() ? config.getSprintLoss().get() : 0.0) + (1.0 - environment.getHumidity()) * config.getDryAirLoss().get();
		return loseHydration(state, loss, seconds);
	}

	/**
	 * Baseline and sprinting gameplay with no environmental multiplier.
	 */
	public ThirstState advanceActivePlay (@NonNull ThirstState state, boolean sprinting, double seconds) {
		return advanceActivePlay(state, sprinting, 1.0, seconds);
	}

	/**
	 * The coordinator supplies a numeric temperature contribution without coupling feature simulations.
	 */
	public ThirstState advanceActivePlay (@NonNull ThirstState state, boolean sprinting, double lossMultiplier, double seconds) {
		SurvivalMath.requireRange(lossMultiplier, 1.0, 20.0, "lossMultiplier");
		double loss = (config.getBaseLoss().get() + (sprinting ? config.getSprintLoss().get() : 0.0)) * lossMultiplier;
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
