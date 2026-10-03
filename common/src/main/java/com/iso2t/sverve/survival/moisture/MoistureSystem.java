package com.iso2t.sverve.survival.moisture;

import com.iso2t.sverve.survival.SurvivalMath;
import com.iso2t.sverve.survival.environment.EnvironmentSample;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class MoistureSystem {
	@NonNull
	private final MoistureConfig config;

	public MoistureState advance (@NonNull MoistureState state, @NonNull EnvironmentSample environment, double seconds) {
		SurvivalMath.requireRange(seconds, 0.0, Double.MAX_VALUE, "seconds");
		if (!config.getEnabled().get() || seconds == 0.0) return state;
		if (environment.isImmersed()) return new MoistureState(1.0);

		double rate = environment.isExposedToRain() ? config.getRainGain().get() : -dryingRate(environment);
		return new MoistureState(SurvivalMath.clamp(state.getWetness() + rate * seconds, 0.0, 1.0));
	}

	public double coolingWetness (@NonNull MoistureState state) {
		return config.getEnabled().get() ? state.getWetness() : 0.0;
	}

	private double dryingRate (EnvironmentSample environment) {
		double heat = Math.max(0.0, environment.getAmbientTemperature());
		double dryAir = 1.0 - environment.getHumidity();
		return (config.getDryingRate().get() + heat * config.getHeatDryingRate().get()) * dryAir;
	}
}
