package com.iso2t.sverve.survival.temperature;

import com.iso2t.sverve.survival.SurvivalMath;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Converts any finite vanilla biome climate value into Sverve's bounded exposure units.
 */
@RequiredArgsConstructor
public final class BiomeTemperatureMapping {
	@NonNull
	private final TemperatureConfig config;

	public double map (double biomeTemperature) {
		SurvivalMath.requireRange(biomeTemperature, -Double.MAX_VALUE, Double.MAX_VALUE, "biomeTemperature");
		double difference = biomeTemperature - config.getComfortableBiomeTemperature().get();
		double range = difference < 0.0 ? config.getColdBiomeRange().get() : config.getHotBiomeRange().get();
		return SurvivalMath.clamp(difference / range, -1.0, 1.0);
	}
}
