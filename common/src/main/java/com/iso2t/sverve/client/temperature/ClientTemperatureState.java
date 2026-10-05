package com.iso2t.sverve.client.temperature;

import com.iso2t.sverve.network.temperature.TemperatureSnapshot;
import lombok.Getter;
import lombok.NonNull;

@Getter
public final class ClientTemperatureState {

	private TemperatureSnapshot snapshot;

	public void accept (@NonNull TemperatureSnapshot snapshot) {
		this.snapshot = snapshot;
	}

	public void clear () {
		snapshot = null;
	}
}
