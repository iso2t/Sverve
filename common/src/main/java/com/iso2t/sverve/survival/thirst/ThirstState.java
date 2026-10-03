package com.iso2t.sverve.survival.thirst;

import com.iso2t.sverve.survival.SurvivalMath;
import lombok.Value;

@Value
public class ThirstState {
	/**
	 * 0 dehydrated, 1 fully hydrated.
	 */
	double hydration;

	public ThirstState (double hydration) {
		this.hydration = SurvivalMath.requireRange(hydration, 0.0, 1.0, "hydration");
	}

	public static ThirstState hydrated () {
		return new ThirstState(1.0);
	}

	/**
	 * Amount is a normalized fraction; an authoritative drink handler should call this.
	 */
	public ThirstState drink (double amount) {
		SurvivalMath.requireRange(amount, 0.0, 1.0, "drink amount");
		return new ThirstState(SurvivalMath.clamp(hydration + amount, 0.0, 1.0));
	}
}
