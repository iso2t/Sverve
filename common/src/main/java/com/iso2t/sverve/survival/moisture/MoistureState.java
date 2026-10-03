package com.iso2t.sverve.survival.moisture;

import com.iso2t.sverve.survival.SurvivalMath;
import lombok.Value;

@Value
public class MoistureState {
	/**
	 * 0 dry, 1 soaked.
	 */
	double wetness;

	public MoistureState (double wetness) {
		this.wetness = SurvivalMath.requireRange(wetness, 0.0, 1.0, "wetness");
	}

	public static MoistureState dry () {
		return new MoistureState(0.0);
	}
}
