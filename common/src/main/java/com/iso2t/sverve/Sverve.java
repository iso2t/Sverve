package com.iso2t.sverve;

import com.iso2t.easyconfig.api.ConfigBuildOptions;
import com.iso2t.easyconfig.api.ConfigBuilder;
import com.iso2t.easyconfig.api.files.FileTypes;
import com.iso2t.sverve.config.SurvivalConfig;
import com.iso2t.sverve.network.moisture.MoistureSyncTransport;
import com.iso2t.sverve.network.moisture.MoistureSynchronizer;
import com.iso2t.sverve.network.temperature.TemperatureSyncTransport;
import com.iso2t.sverve.network.temperature.TemperatureSynchronizer;
import com.iso2t.sverve.network.thirst.ThirstSyncTransport;
import com.iso2t.sverve.network.thirst.ThirstSynchronizer;
import com.iso2t.sverve.platform.Services;
import com.iso2t.sverve.player.environment.BiomeEnvironmentSampler;
import com.iso2t.sverve.player.environment.NearbyHeatSampler;
import com.iso2t.sverve.player.moisture.MoistureGameplay;
import com.iso2t.sverve.player.moisture.MoistureStorage;
import com.iso2t.sverve.player.moisture.PlayerMoisture;
import com.iso2t.sverve.player.temperature.*;
import com.iso2t.sverve.player.thirst.PlayerThirst;
import com.iso2t.sverve.player.thirst.ThirstGameplay;
import com.iso2t.sverve.player.thirst.ThirstPenalties;
import com.iso2t.sverve.player.thirst.ThirstStorage;
import com.iso2t.sverve.survival.SurvivalEngine;
import com.iso2t.sverve.survival.moisture.MoistureSystem;
import com.iso2t.sverve.survival.temperature.BiomeTemperatureMapping;
import com.iso2t.sverve.survival.temperature.TemperatureMetabolismSystem;
import com.iso2t.sverve.survival.temperature.TemperatureSystem;
import com.iso2t.sverve.survival.thirst.ThirstSystem;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Shared composition root; gameplay belongs in the feature packages.
 */
@UtilityClass
public class Sverve {
	public static SverveRuntime initialize (@NonNull ThirstStorage storage, @NonNull ThirstSyncTransport transport, @NonNull TemperatureStorage temperatureStorage, @NonNull TemperatureSyncTransport temperatureTransport, @NonNull MoistureStorage moistureStorage, @NonNull MoistureSyncTransport moistureTransport) {
		SurvivalConfig config = ConfigBuilder.build(SurvivalConfig.class, Constants.MOD_ID, FileTypes.TOML, ConfigBuildOptions.defaults().screenTitle("Survival"));
		Constants.LOG.info("Initializing {} on {} ({})", Constants.MOD_NAME, Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
		var playerThirst = new PlayerThirst(storage);
		var synchronizer = new ThirstSynchronizer(playerThirst, config.getThirst(), transport);
		var playerTemperature = new PlayerTemperature(temperatureStorage);
		var playerMoisture = new PlayerMoisture(moistureStorage);
		var moistureSynchronizer = new MoistureSynchronizer(playerMoisture, config.getMoisture(), moistureTransport);
		var moistureGameplay = new MoistureGameplay(playerMoisture, moistureStorage, config.getMoisture(), new MoistureSystem(config.getMoisture()), moistureSynchronizer);
		var metabolism = new TemperatureMetabolism(playerTemperature, new TemperatureMetabolismSystem(config.getTemperature()));
		var temperatureSynchronizer = new TemperatureSynchronizer(playerTemperature, config.getTemperature(), temperatureTransport);
		var environment = new BiomeEnvironmentSampler(new BiomeTemperatureMapping(config.getTemperature()), new NearbyHeatSampler(config.getHeatSources()));
		var temperatureGameplay = new TemperatureGameplay(playerTemperature, temperatureStorage, config.getTemperature(), new TemperatureSystem(config.getTemperature()), environment, temperatureSynchronizer, new TemperaturePenalties(temperatureStorage, config.getTemperature()), metabolism, moistureGameplay);
		return new SverveRuntime(config, SurvivalEngine.create(config), playerThirst, new ThirstGameplay(playerThirst, new ThirstSystem(config.getThirst()), synchronizer, new ThirstPenalties(storage, config.getThirst()), metabolism, environment), synchronizer, playerTemperature, temperatureGameplay, temperatureSynchronizer, metabolism, playerMoisture, moistureGameplay, moistureSynchronizer);
	}
}
