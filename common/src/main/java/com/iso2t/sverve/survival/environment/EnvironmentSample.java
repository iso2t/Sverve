package com.iso2t.sverve.survival.environment;

import com.iso2t.sverve.survival.SurvivalMath;
import lombok.Builder;
import lombok.Value;

/**
 * Inputs already resolved by a world adapter; no world queries occur in the simulation.
 */
@Value
public class EnvironmentSample {
	/**
	 * Normalized exposure: -1 freezing, 0 temperate, 1 extreme heat; not degrees Celsius.
	 */
	double  ambientTemperature;
	/**
	 * Relative humidity: 0 dry air, 1 saturated air.
	 */
	double  humidity;
	boolean exposedToRain;
	boolean immersed;
	boolean sprinting;

	@Builder
	public EnvironmentSample (double ambientTemperature, double humidity, boolean exposedToRain, boolean immersed, boolean sprinting) {
		this.ambientTemperature = SurvivalMath.requireRange(ambientTemperature, -1.0, 1.0, "ambientTemperature");
		this.humidity = SurvivalMath.requireRange(humidity, 0.0, 1.0, "humidity");
		this.exposedToRain = exposedToRain;
		this.immersed = immersed;
		this.sprinting = sprinting;
	}

	public static EnvironmentSample temperate () {
		return new EnvironmentSample(0.0, 0.5, false, false, false);
	}
}
