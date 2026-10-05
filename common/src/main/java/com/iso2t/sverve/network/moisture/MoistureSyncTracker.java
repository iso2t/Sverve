package com.iso2t.sverve.network.moisture;

import lombok.NonNull;

public final class MoistureSyncTracker {

	private MoistureSnapshot lastSent;

	public boolean needsUpdate (@NonNull MoistureSnapshot snapshot) {
		return !snapshot.equals(lastSent);
	}

	public void markSent (@NonNull MoistureSnapshot snapshot) {
		lastSent = snapshot;
	}
}
