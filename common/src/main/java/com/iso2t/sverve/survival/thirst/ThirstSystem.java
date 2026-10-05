package com.iso2t.sverve.survival.thirst;

import com.iso2t.sverve.config.ThirstConfig;
import com.iso2t.sverve.survival.Mth;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ThirstSystem {

	@NonNull
	private final ThirstConfig config;

	public ThirstState advance (@NonNull ThirstState state, boolean sprinting, double humidity, double bodyHeat, double seconds) {
		Mth.requireRange(bodyHeat, 0.0, 1.0, "bodyHeat");
		Mth.requireRange(humidity, 0.0, 1.0, "humidity");
		var loss = config.getBaseLoss().get() + bodyHeat * config.getHeatLoss().get() + (sprinting ? config.getSprintLoss().get() : 0.0) + (1.0 - humidity) * config.getDryAirLoss().get();
		return loseHydration(state, loss, seconds);
	}

	public ThirstState drinkWater (@NonNull ThirstState state) {
		return config.getEnabled().get() ? state.drink(config.getWaterBottleHydration().get()) : state;
	}

	public ThirstState exert (@NonNull ThirstState state, double waterLoss) {
		Mth.requireRange(waterLoss, 0.0, Double.MAX_VALUE, "waterLoss");
		return loseHydration(state, waterLoss, 1.0);
	}

	private ThirstState loseHydration (ThirstState state, double loss, double seconds) {
		Mth.requireRange(seconds, 0.0, Double.MAX_VALUE, "seconds");
		if (!config.getEnabled().get() || seconds == 0.0 || loss == 0.0 || state.hydration() == 0.0) return state;
		return new ThirstState(Mth.clamp(state.hydration() - loss * seconds, 0.0, 1.0));
	}
}
