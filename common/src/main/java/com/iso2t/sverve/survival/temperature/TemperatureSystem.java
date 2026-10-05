package com.iso2t.sverve.survival.temperature;

import com.iso2t.sverve.config.TemperatureConfig;
import com.iso2t.sverve.survival.Mth;
import com.iso2t.sverve.survival.environment.EnvironmentSample;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class TemperatureSystem {

	@NonNull
	private final TemperatureConfig config;

	public TemperatureState advance (@NonNull TemperatureState state, @NonNull EnvironmentSample environment, double wetness, double seconds) {
		return advance(state, environment, wetness, TemperatureProtectionScore.NONE, seconds);
	}

	public TemperatureState advance (@NonNull TemperatureState state, @NonNull EnvironmentSample environment, double wetness, @NonNull TemperatureProtectionScore protection, double seconds) {
		Mth.requireRange(wetness, 0.0, 1.0, "wetness");
		Mth.requireRange(seconds, 0.0, Double.MAX_VALUE, "seconds");
		if (!config.getEnabled().get() || seconds == 0.0) return state;

		var target = Mth.clamp(environment.ambientTemperature() + environment.nearbyHeat() - wetness * config.getWetCooling().get(), -1.0, 1.0);
		target = protection.insulate(target);

		var response = -Math.expm1(-config.getResponseRate().get() * seconds);
		var exposure = state.exposure() + (target - state.exposure()) * response;
		return new TemperatureState(Mth.clamp(exposure, -1.0, 1.0));
	}
}
