package com.iso2t.sverve.survival.temperature;

import com.iso2t.sverve.survival.SurvivalMath;
import com.iso2t.sverve.survival.environment.EnvironmentSample;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class TemperatureSystem {
	@NonNull
	private final TemperatureConfig config;

	public TemperatureState advance (@NonNull TemperatureState state, @NonNull EnvironmentSample environment, double wetness, double seconds) {
		SurvivalMath.requireRange(wetness, 0.0, 1.0, "wetness");
		SurvivalMath.requireRange(seconds, 0.0, Double.MAX_VALUE, "seconds");
		if (!config.getEnabled().get() || seconds == 0.0) return state;

		double target = SurvivalMath.clamp(environment.getAmbientTemperature() + environment.getNearbyHeat() - wetness * config.getWetCooling().get(), -1.0, 1.0);
		// Exponential relaxation approaches the target without overshooting on long updates.
		double response = -Math.expm1(-config.getResponseRate().get() * seconds);
		double exposure = state.getExposure() + (target - state.getExposure()) * response;
		return new TemperatureState(SurvivalMath.clamp(exposure, -1.0, 1.0));
	}

	public double thirstHeat (@NonNull TemperatureState state) {
		return config.getEnabled().get() ? Math.max(0.0, state.getExposure()) : 0.0;
	}
}
