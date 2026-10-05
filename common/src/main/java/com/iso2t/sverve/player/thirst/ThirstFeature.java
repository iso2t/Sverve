package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.network.thirst.ThirstSyncTransport;
import com.iso2t.sverve.network.thirst.ThirstSynchronizer;
import com.iso2t.sverve.player.environment.EnvironmentSampler;
import com.iso2t.sverve.player.exertion.MovementThirst;
import com.iso2t.sverve.player.temperature.TemperatureMetabolism;
import com.iso2t.sverve.config.LoadThirstConfig;
import com.iso2t.sverve.survival.exertion.LoadThirstCost;
import com.iso2t.sverve.config.ThirstConfig;
import com.iso2t.sverve.survival.thirst.ThirstSystem;
import lombok.NonNull;

public record ThirstFeature(@NonNull PlayerThirst players, @NonNull ThirstGameplay gameplay, @NonNull ThirstSynchronizer synchronizer) {

	public static ThirstFeature create (ThirstConfig config, LoadThirstConfig loadConfig, ThirstStorage storage, ThirstSyncTransport transport, TemperatureMetabolism metabolism, EnvironmentSampler environment) {
		var players = new PlayerThirst(storage);
		var synchronizer = new ThirstSynchronizer(players, config, transport);
		var penalties = new ThirstPenalties(storage, config);
		var movement = new MovementThirst(new LoadThirstCost(loadConfig));
		var gameplay = new ThirstGameplay(players, new ThirstSystem(config), synchronizer, penalties, metabolism, environment, movement);
		return new ThirstFeature(players, gameplay, synchronizer);
	}
}
