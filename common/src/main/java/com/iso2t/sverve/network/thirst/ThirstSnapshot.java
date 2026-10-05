package com.iso2t.sverve.network.thirst;

import com.iso2t.sverve.survival.thirst.ThirstState;
import lombok.NonNull;

public record ThirstSnapshot(int halfUnits, boolean enabled) {

	public static final int MAX_HALF_UNITS = 20;

	public ThirstSnapshot {
		if (halfUnits < 0 || halfUnits > MAX_HALF_UNITS) {
			throw new IllegalArgumentException("halfUnits must be between 0 and 20");
		}
	}

	public static ThirstSnapshot from (@NonNull ThirstState state, boolean enabled) {
		return new ThirstSnapshot((int) Math.ceil(state.hydration() * MAX_HALF_UNITS), enabled);
	}
}
