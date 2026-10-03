package com.iso2t.sverve.survival;

import com.iso2t.sverve.survival.moisture.MoistureState;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import com.iso2t.sverve.survival.thirst.ThirstState;
import lombok.NonNull;
import lombok.Value;

/**
 * Immutable state for one player. Storage and synchronization belong to adapters.
 */
@Value
public class SurvivalState {
	@NonNull
	TemperatureState temperature;
	@NonNull
	MoistureState    moisture;
	@NonNull
	ThirstState      thirst;

	public static SurvivalState healthy () {
		return new SurvivalState(TemperatureState.comfortable(), MoistureState.dry(), ThirstState.hydrated());
	}
}
