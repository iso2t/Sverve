package com.iso2t.sverve.survival.temperature;

import com.iso2t.sverve.survival.Mth;

public record TemperatureState(double exposure) {

	public TemperatureState (double exposure) {
		this.exposure = Mth.requireRange(exposure, -1.0, 1.0, "exposure");
	}

	public static TemperatureState comfortable () {
		return new TemperatureState(0.0);
	}
}
