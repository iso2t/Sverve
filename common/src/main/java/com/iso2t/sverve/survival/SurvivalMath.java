package com.iso2t.sverve.survival;

import lombok.experimental.UtilityClass;

/**
 * Numeric invariants shared by survival values.
 */
@UtilityClass
public class SurvivalMath {
	public static double requireRange (double value, double min, double max, String name) {
		if (!Double.isFinite(value) || value < min || value > max) {
			throw new IllegalArgumentException(name + " must be finite and between " + min + " and " + max);
		}
		return value;
	}

	public static double clamp (double value, double min, double max) {
		return Math.clamp(value, min, max);
	}
}
