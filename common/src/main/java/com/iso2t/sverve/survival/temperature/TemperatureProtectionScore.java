package com.iso2t.sverve.survival.temperature;

import com.iso2t.sverve.survival.Mth;

public record TemperatureProtectionScore(double coldReduction, double heatReduction) {

	public static final TemperatureProtectionScore NONE = new TemperatureProtectionScore(0, 0);

	public TemperatureProtectionScore (double coldReduction, double heatReduction) {
		this.coldReduction = Mth.requireRange(coldReduction, 0, 1, "coldReduction");
		this.heatReduction = Mth.requireRange(heatReduction, 0, 1, "heatReduction");
	}

	public static TemperatureProtectionScore fromLevels (int insulation, int heatProtection, int thermalProtection) {
		if (insulation < 0 || heatProtection < 0 || thermalProtection < 0) {
			throw new IllegalArgumentException("Protection levels cannot be negative");
		}
		var universal = thermalProtection * 0.5;
		return new TemperatureProtectionScore(Math.min(1, (insulation + universal) / 16), Math.min(1, (heatProtection + universal) / 16));
	}

	public double insulate (double exposure) {
		Mth.requireRange(exposure, -1, 1, "exposure");
		return exposure * (1 - (exposure < 0 ? coldReduction : heatReduction));
	}
}
