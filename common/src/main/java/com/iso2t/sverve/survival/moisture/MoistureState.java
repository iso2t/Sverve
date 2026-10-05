package com.iso2t.sverve.survival.moisture;

import com.iso2t.sverve.survival.Mth;

public record MoistureState(double wetness) {

	public MoistureState (double wetness) {
		this.wetness = Mth.requireRange(wetness, 0.0, 1.0, "wetness");
	}

	public static MoistureState dry () {
		return new MoistureState(0.0);
	}
}
