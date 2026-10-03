package com.iso2t.sverve.network.moisture;

import lombok.NonNull;

/**
 * Transient per-owner delivery state. Failed sends remain eligible for retry.
 */
public final class MoistureSyncTracker {
	private MoistureSnapshot lastSent;

	public boolean needsUpdate (@NonNull MoistureSnapshot snapshot) {
		return !snapshot.equals(lastSent);
	}

	public void markSent (@NonNull MoistureSnapshot snapshot) {
		lastSent = snapshot;
	}
}
