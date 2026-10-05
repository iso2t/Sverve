package com.iso2t.sverve.survival.environment;

import com.iso2t.sverve.survival.Mth;
import lombok.Builder;

public record EnvironmentSample(double ambientTemperature, double nearbyHeat, double humidity, boolean exposedToRain, boolean immersed) {

	@Builder
	public EnvironmentSample (double ambientTemperature, double nearbyHeat, double humidity, boolean exposedToRain, boolean immersed) {
		this.ambientTemperature = Mth.requireRange(ambientTemperature, -1.0, 1.0, "ambientTemperature");
		this.nearbyHeat = Mth.requireRange(nearbyHeat, 0.0, 1.0, "nearbyHeat");
		this.humidity = Mth.requireRange(humidity, 0.0, 1.0, "humidity");
		this.exposedToRain = exposedToRain;
		this.immersed = immersed;
	}

	public static EnvironmentSample temperate () {
		return EnvironmentSample.builder().humidity(0.5).build();
	}
}
