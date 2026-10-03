package com.iso2t.sverve.survival.temperature;

import com.iso2t.sverve.survival.SurvivalMath;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Ordered display bands. Hysteresis is presentation policy, never a damage threshold.
 */
@Getter
@RequiredArgsConstructor
public enum TemperatureBand {
	FREEZING(0),
	COLD(1),
	NORMAL(2),
	WARM(3),
	HOT(4);

	public static final  double   HYSTERESIS = 0.03;
	private static final double[] BOUNDARIES = { -0.75, -0.25, 0.25, 0.75 };
	private final        int      id;

	public static TemperatureBand fromId (int id) {
		return switch (id) {
			case 0 -> FREEZING;
			case 1 -> COLD;
			case 2 -> NORMAL;
			case 3 -> WARM;
			case 4 -> HOT;
			default -> throw new IllegalArgumentException("Unknown temperature band: " + id);
		};
	}

	public static TemperatureBand classify (double exposure) {
		SurvivalMath.requireRange(exposure, -1.0, 1.0, "exposure");
		if (exposure <= -0.75) return FREEZING;
		if (exposure < -0.25) return COLD;
		if (exposure <= 0.25) return NORMAL;
		if (exposure < 0.75) return WARM;
		return HOT;
	}

	public static TemperatureBand stabilize (TemperatureBand previous, double exposure) {
		SurvivalMath.requireRange(exposure, -1.0, 1.0, "exposure");
		if (previous == null) return classify(exposure);
		int index = previous.ordinal();
		while (index > 0 && exposure < BOUNDARIES[index - 1] - HYSTERESIS) index--;
		while (index < BOUNDARIES.length && exposure > BOUNDARIES[index] + HYSTERESIS) index++;
		return values()[index];
	}
}
