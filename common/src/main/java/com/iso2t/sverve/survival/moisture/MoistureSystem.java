package com.iso2t.sverve.survival.moisture;

import com.iso2t.sverve.config.MoistureConfig;
import com.iso2t.sverve.survival.Mth;
import com.iso2t.sverve.survival.environment.EnvironmentSample;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class MoistureSystem {

	@NonNull
	private final MoistureConfig config;

	public MoistureState advance (@NonNull MoistureState state, @NonNull EnvironmentSample environment, double seconds) {
		Mth.requireRange(seconds, 0.0, Double.MAX_VALUE, "seconds");
		if (!config.getEnabled().get() || seconds == 0.0) return state;
		if (environment.immersed()) return new MoistureState(1.0);

		var rate = environment.exposedToRain() ? config.getRainGain().get() : -dryingRate(environment);
		return new MoistureState(Mth.clamp(state.wetness() + rate * seconds, 0.0, 1.0));
	}

	public double coolingWetness (@NonNull MoistureState state) {
		return config.getEnabled().get() ? state.wetness() : 0.0;
	}

	private double dryingRate (EnvironmentSample environment) {
		var heat = Math.max(0.0, environment.ambientTemperature()) + environment.nearbyHeat();

		var humidityFactor = 1.0 - 0.75 * environment.humidity();
		return (config.getDryingRate().get() + heat * config.getHeatDryingRate().get()) * humidityFactor;
	}
}
