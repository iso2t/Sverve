package com.iso2t.sverve.survival.thirst;

import com.iso2t.sverve.survival.Mth;

public record ThirstState(double hydration) {

	public ThirstState (double hydration) {
		this.hydration = Mth.requireRange(hydration, 0.0, 1.0, "hydration");
	}

	public static ThirstState hydrated () {
		return new ThirstState(1.0);
	}

	public ThirstState drink (double amount) {
		Mth.requireRange(amount, 0.0, 1.0, "drink amount");
		return new ThirstState(Mth.clamp(hydration + amount, 0.0, 1.0));
	}
}
