package com.iso2t.sverve.network.temperature;

import com.iso2t.sverve.survival.temperature.TemperatureBand;
import lombok.NonNull;

public final class TemperatureSyncTracker {

	private TemperatureSnapshot lastSent;

	public TemperatureSnapshot snapshot (double exposure, boolean enabled, boolean force) {
		var previous = force || lastSent == null ? null : lastSent.band();
		return new TemperatureSnapshot(TemperatureBand.stabilize(previous, exposure), enabled);
	}

	public boolean needsUpdate (@NonNull TemperatureSnapshot snapshot) {
		return !snapshot.equals(lastSent);
	}

	public void markSent (@NonNull TemperatureSnapshot snapshot) {
		lastSent = snapshot;
	}
}
