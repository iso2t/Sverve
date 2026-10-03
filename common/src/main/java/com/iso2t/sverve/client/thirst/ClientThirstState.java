package com.iso2t.sverve.client.thirst;

import com.iso2t.sverve.network.thirst.ThirstSnapshot;
import lombok.Getter;
import lombok.NonNull;

/**
 * One client connection's received view. No prediction, gameplay updates, or config loading.
 */
@Getter
public final class ClientThirstState {
	private ThirstSnapshot snapshot;

	public void accept (@NonNull ThirstSnapshot snapshot) {
		this.snapshot = snapshot;
	}

	public void clear () {
		snapshot = null;
	}
}
