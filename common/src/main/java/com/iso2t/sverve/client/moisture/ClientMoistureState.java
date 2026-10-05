package com.iso2t.sverve.client.moisture;

import com.iso2t.sverve.network.moisture.MoistureSnapshot;
import lombok.Getter;
import lombok.NonNull;

@Getter
public final class ClientMoistureState {

	private MoistureSnapshot snapshot;

	public void accept (@NonNull MoistureSnapshot snapshot) {
		this.snapshot = snapshot;
	}

	public void clear () {
		snapshot = null;
	}
}
