package com.iso2t.sverve.network.temperature;

import com.iso2t.sverve.survival.temperature.TemperatureBand;
import lombok.NonNull;

/**
 * Transient owner-delivery state. A failed send never advances the hysteresis baseline.
 */
public final class TemperatureSyncTracker {
	private TemperatureSnapshot lastSent;

	public TemperatureSnapshot snapshot (double exposure, boolean enabled, boolean force) {
		var previous = force || lastSent == null ? null : lastSent.getBand();
		return new TemperatureSnapshot(TemperatureBand.stabilize(previous, exposure), enabled);
	}

	public boolean needsUpdate (@NonNull TemperatureSnapshot snapshot) {
		return !snapshot.equals(lastSent);
	}

	public void markSent (@NonNull TemperatureSnapshot snapshot) {
		lastSent = snapshot;
	}
}
