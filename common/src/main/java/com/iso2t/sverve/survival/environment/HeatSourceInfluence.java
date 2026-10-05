package com.iso2t.sverve.survival.environment;

import com.iso2t.sverve.survival.Mth;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HeatSourceInfluence {

	public static double atDistance (double distance, double radius, double warmth) {
		Mth.requireRange(distance, 0.0, Double.MAX_VALUE, "distance");
		Mth.requireRange(radius, 1.0, 8.0, "radius");
		Mth.requireRange(warmth, 0.0, 1.0, "warmth");
		return warmth * Math.max(0.0, 1.0 - distance / radius);
	}
}
