package com.iso2t.sverve.network.thirst;

import lombok.NonNull;

/**
 * Transient per-player attachment. Never saved to disk or copied on death.
 */
public final class ThirstSyncTracker {
	private ThirstSnapshot lastSent;

	public boolean needsUpdate (@NonNull ThirstSnapshot snapshot) {
		return !snapshot.equals(lastSent);
	}

	public void markSent (@NonNull ThirstSnapshot snapshot) {
		lastSent = snapshot;
	}
}
