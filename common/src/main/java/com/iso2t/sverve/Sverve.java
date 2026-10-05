package com.iso2t.sverve;

import com.iso2t.easyconfig.api.ConfigBuildOptions;
import com.iso2t.easyconfig.api.ConfigBuilder;
import com.iso2t.easyconfig.api.files.FileTypes;
import com.iso2t.sverve.config.SurvivalConfig;
import com.iso2t.sverve.network.moisture.MoistureSyncTransport;
import com.iso2t.sverve.network.temperature.TemperatureSyncTransport;
import com.iso2t.sverve.network.thirst.ThirstSyncTransport;
import com.iso2t.sverve.platform.Services;
import com.iso2t.sverve.player.environment.EnvironmentSampler;
import com.iso2t.sverve.player.environment.NearbyHeatSampler;
import com.iso2t.sverve.player.moisture.MoistureFeature;
import com.iso2t.sverve.player.moisture.MoistureStorage;
import com.iso2t.sverve.player.temperature.TemperatureFeature;
import com.iso2t.sverve.player.temperature.TemperatureStorage;
import com.iso2t.sverve.player.thirst.ThirstFeature;
import com.iso2t.sverve.player.thirst.ThirstStorage;
import com.iso2t.sverve.survival.temperature.BiomeTemperatureMapping;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Sverve {

	public static SverveRuntime initialize (@NonNull ThirstStorage thirstStorage, @NonNull ThirstSyncTransport thirstTransport, @NonNull TemperatureStorage temperatureStorage, @NonNull TemperatureSyncTransport temperatureTransport, @NonNull MoistureStorage moistureStorage, @NonNull MoistureSyncTransport moistureTransport) {
		Constants.LOG.info("Initializing {} on {} ({})", Constants.MOD_NAME, Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
		var config = loadConfig();
		var environment = new EnvironmentSampler(new BiomeTemperatureMapping(config.getTemperature()), new NearbyHeatSampler(config.getHeatSources()));
		var moisture = MoistureFeature.create(config.getMoisture(), moistureStorage, moistureTransport);
		var temperature = TemperatureFeature.create(config.getTemperature(), temperatureStorage, temperatureTransport, environment, moisture.gameplay());
		var thirst = ThirstFeature.create(config.getThirst(), config.getHeavyInventories(), thirstStorage, thirstTransport, temperature.metabolism(), environment);
		return new SverveRuntime(config, thirst, temperature, moisture);
	}

	private static SurvivalConfig loadConfig () {
		return ConfigBuilder.build(SurvivalConfig.class, Constants.MOD_ID, FileTypes.TOML, ConfigBuildOptions.defaults().screenTitle("Survival"));
	}
}
