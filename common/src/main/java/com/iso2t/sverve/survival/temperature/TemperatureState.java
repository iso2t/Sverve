package com.iso2t.sverve.survival.temperature;

import com.iso2t.sverve.survival.SurvivalMath;
import lombok.Value;

@Value
public class TemperatureState {
	/**
	 * Normalized body exposure: -1 freezing, 0 comfortable, 1 overheating.
	 */
	double exposure;

	public TemperatureState (double exposure) {
		this.exposure = SurvivalMath.requireRange(exposure, -1.0, 1.0, "exposure");
	}

	public static TemperatureState comfortable () {
		return new TemperatureState(0.0);
	}
}
