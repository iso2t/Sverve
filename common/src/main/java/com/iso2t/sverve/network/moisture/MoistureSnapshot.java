package com.iso2t.sverve.network.moisture;

import com.iso2t.sverve.survival.moisture.MoistureState;
import lombok.NonNull;

public record MoistureSnapshot(int fillUnits, boolean enabled) {

	public static final int MAX_FILL_UNITS = 7;

	public MoistureSnapshot {
		if (fillUnits < 0 || fillUnits > MAX_FILL_UNITS) {
			throw new IllegalArgumentException("fillUnits must be between 0 and 7");
		}
	}

	public static MoistureSnapshot from (@NonNull MoistureState state, boolean enabled) {
		return new MoistureSnapshot((int) Math.ceil(state.wetness() * MAX_FILL_UNITS), enabled);
	}
}
