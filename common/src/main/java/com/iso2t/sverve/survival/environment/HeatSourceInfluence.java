package com.iso2t.sverve.survival.environment;

import com.iso2t.sverve.survival.SurvivalMath;
import lombok.experimental.UtilityClass;

/**
 * A source contributes linearly less warmth with distance; the sampler selects the strongest.
 */
@UtilityClass
public class HeatSourceInfluence {
	public static double atDistance (double distance, double radius, double warmth) {
		SurvivalMath.requireRange(distance, 0.0, Double.MAX_VALUE, "distance");
		SurvivalMath.requireRange(radius, 1.0, 8.0, "radius");
		SurvivalMath.requireRange(warmth, 0.0, 1.0, "warmth");
		return warmth * Math.max(0.0, 1.0 - distance / radius);
	}
}
