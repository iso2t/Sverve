package com.iso2t.sverve;

import com.iso2t.sverve.config.SurvivalConfig;
import com.iso2t.sverve.network.moisture.MoistureSynchronizer;
import com.iso2t.sverve.network.temperature.TemperatureSynchronizer;
import com.iso2t.sverve.network.thirst.ThirstSynchronizer;
import com.iso2t.sverve.player.moisture.MoistureGameplay;
import com.iso2t.sverve.player.moisture.PlayerMoisture;
import com.iso2t.sverve.player.temperature.PlayerTemperature;
import com.iso2t.sverve.player.temperature.TemperatureGameplay;
import com.iso2t.sverve.player.temperature.TemperatureMetabolism;
import com.iso2t.sverve.player.thirst.PlayerThirst;
import com.iso2t.sverve.player.thirst.ThirstGameplay;
import com.iso2t.sverve.survival.SurvivalEngine;
import lombok.NonNull;
import lombok.Value;

/**
 * Shared services constructed once per loader initialization and passed to native hooks.
 */
@Value
public class SverveRuntime {
	@NonNull
	SurvivalConfig          config;
	@NonNull
	SurvivalEngine          survival;
	@NonNull
	PlayerThirst            playerThirst;
	@NonNull
	ThirstGameplay          thirstGameplay;
	@NonNull
	ThirstSynchronizer      thirstSynchronizer;
	@NonNull
	PlayerTemperature       playerTemperature;
	@NonNull
	TemperatureGameplay     temperatureGameplay;
	@NonNull
	TemperatureSynchronizer temperatureSynchronizer;
	@NonNull
	TemperatureMetabolism   temperatureMetabolism;
	@NonNull
	PlayerMoisture          playerMoisture;
	@NonNull
	MoistureGameplay        moistureGameplay;
	@NonNull
	MoistureSynchronizer    moistureSynchronizer;
}
