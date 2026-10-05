package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.network.temperature.TemperatureSyncTransport;
import com.iso2t.sverve.network.temperature.TemperatureSynchronizer;
import com.iso2t.sverve.player.environment.EnvironmentSampler;
import com.iso2t.sverve.player.moisture.MoistureGameplay;
import com.iso2t.sverve.config.TemperatureConfig;
import com.iso2t.sverve.survival.temperature.TemperatureMetabolismSystem;
import com.iso2t.sverve.survival.temperature.TemperatureSystem;
import lombok.NonNull;

public record TemperatureFeature(@NonNull PlayerTemperature players, @NonNull TemperatureGameplay gameplay, @NonNull TemperatureSynchronizer synchronizer, @NonNull TemperatureMetabolism metabolism) {

	public static TemperatureFeature create (TemperatureConfig config, TemperatureStorage storage, TemperatureSyncTransport transport, EnvironmentSampler environment, MoistureGameplay moisture) {
		var players = new PlayerTemperature(storage);
		var synchronizer = new TemperatureSynchronizer(players, config, transport);
		var metabolism = new TemperatureMetabolism(players, new TemperatureMetabolismSystem(config));
		var penalties = new TemperaturePenalties(storage, config);
		var gameplay = new TemperatureGameplay(players, storage, config, new TemperatureSystem(config), environment, synchronizer, penalties, metabolism, moisture);
		return new TemperatureFeature(players, gameplay, synchronizer, metabolism);
	}
}
