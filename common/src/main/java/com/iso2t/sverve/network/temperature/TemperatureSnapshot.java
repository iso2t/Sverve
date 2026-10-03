package com.iso2t.sverve.network.temperature;

import com.iso2t.sverve.survival.temperature.TemperatureBand;
import lombok.NonNull;
import lombok.Value;

/**
 * Only the server-selected display band and enable flag cross the network.
 */
@Value
public class TemperatureSnapshot {
	@NonNull
	TemperatureBand band;
	boolean enabled;
}
