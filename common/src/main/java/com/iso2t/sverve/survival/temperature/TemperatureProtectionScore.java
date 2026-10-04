package com.iso2t.sverve.survival.temperature;

import com.iso2t.sverve.survival.SurvivalMath;
import lombok.Value;

/**
 * Equipped armor totals; universal levels contribute half a dedicated level.
 */
@Value
public class TemperatureProtectionScore {
	public static final TemperatureProtectionScore NONE = new TemperatureProtectionScore(0, 0);

	double coldReduction;
	double heatReduction;

	public TemperatureProtectionScore (double coldReduction, double heatReduction) {
		this.coldReduction = SurvivalMath.requireRange(coldReduction, 0, 1, "coldReduction");
		this.heatReduction = SurvivalMath.requireRange(heatReduction, 0, 1, "heatReduction");
	}

	public static TemperatureProtectionScore fromLevels (int insulation, int heatProtection, int thermalProtection) {
		if (insulation < 0 || heatProtection < 0 || thermalProtection < 0) {
			throw new IllegalArgumentException("Protection levels cannot be negative");
		}
		double universal = thermalProtection * 0.5;
		return new TemperatureProtectionScore(Math.min(1, (insulation + universal) / 16), Math.min(1, (heatProtection + universal) / 16));
	}

	/**
	 * Applies only the protection for the sign of the supplied temperature.
	 */
	public double insulate (double exposure) {
		SurvivalMath.requireRange(exposure, -1, 1, "exposure");
		return exposure * (1 - (exposure < 0 ? coldReduction : heatReduction));
	}
}
