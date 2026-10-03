package com.iso2t.sverve.network.moisture;

import com.iso2t.sverve.survival.moisture.MoistureState;
import lombok.NonNull;
import lombok.Value;

/**
 * Seven visible fill steps; exact wetness stays on the server.
 */
@Value
public class MoistureSnapshot {
	public static final int MAX_FILL_UNITS = 7;
	int     fillUnits;
	boolean enabled;

	public MoistureSnapshot (int fillUnits, boolean enabled) {
		if (fillUnits < 0 || fillUnits > MAX_FILL_UNITS) {
			throw new IllegalArgumentException("fillUnits must be between 0 and 7");
		}
		this.fillUnits = fillUnits;
		this.enabled = enabled;
	}

	public static MoistureSnapshot from (@NonNull MoistureState state, boolean enabled) {
		return new MoistureSnapshot((int) Math.ceil(state.getWetness() * MAX_FILL_UNITS), enabled);
	}
}
