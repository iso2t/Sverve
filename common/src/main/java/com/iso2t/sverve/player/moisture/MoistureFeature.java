package com.iso2t.sverve.player.moisture;

import com.iso2t.sverve.network.moisture.MoistureSyncTransport;
import com.iso2t.sverve.network.moisture.MoistureSynchronizer;
import com.iso2t.sverve.config.MoistureConfig;
import com.iso2t.sverve.survival.moisture.MoistureSystem;
import lombok.NonNull;

public record MoistureFeature(@NonNull PlayerMoisture players, @NonNull MoistureGameplay gameplay, @NonNull MoistureSynchronizer synchronizer) {

	public static MoistureFeature create (MoistureConfig config, MoistureStorage storage, MoistureSyncTransport transport) {
		var players = new PlayerMoisture(storage);
		var synchronizer = new MoistureSynchronizer(players, config, transport);
		var gameplay = new MoistureGameplay(players, storage, config, new MoistureSystem(config), synchronizer);
		return new MoistureFeature(players, gameplay, synchronizer);
	}
}
