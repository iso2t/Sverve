package com.iso2t.sverve.network.thirst;

import com.iso2t.sverve.survival.thirst.ThirstState;
import lombok.NonNull;
import lombok.Value;

/**
 * HUD precision: ten icons with twenty half steps. The server retains exact hydration.
 */
@Value
public class ThirstSnapshot {
	public static final int MAX_HALF_UNITS = 20;

	int     halfUnits;
	boolean enabled;

	public ThirstSnapshot (int halfUnits, boolean enabled) {
		if (halfUnits < 0 || halfUnits > MAX_HALF_UNITS) {
			throw new IllegalArgumentException("halfUnits must be between 0 and 20");
		}
		this.halfUnits = halfUnits;
		this.enabled = enabled;
	}

	public static ThirstSnapshot from (@NonNull ThirstState state, boolean enabled) {
		return new ThirstSnapshot((int) Math.ceil(state.getHydration() * MAX_HALF_UNITS), enabled);
	}
}
