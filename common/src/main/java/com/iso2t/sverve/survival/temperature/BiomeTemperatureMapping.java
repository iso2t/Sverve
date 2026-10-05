package com.iso2t.sverve.survival.temperature;

import com.iso2t.sverve.config.TemperatureConfig;
import com.iso2t.sverve.survival.Mth;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class BiomeTemperatureMapping {

	@NonNull
	private final TemperatureConfig config;

	public double map (double biomeTemperature) {
		Mth.requireRange(biomeTemperature, -Double.MAX_VALUE, Double.MAX_VALUE, "biomeTemperature");
		var difference = biomeTemperature - config.getComfortableBiomeTemperature().get();
		var range = difference < 0.0 ? config.getColdBiomeRange().get() : config.getHotBiomeRange().get();
		return Mth.clamp(difference / range, -1.0, 1.0);
	}
}
