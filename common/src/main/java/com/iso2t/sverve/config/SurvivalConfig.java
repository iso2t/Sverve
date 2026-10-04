package com.iso2t.sverve.config;

import com.iso2t.easyconfig.api.Side;
import com.iso2t.easyconfig.api.annotations.Comment;
import com.iso2t.easyconfig.api.annotations.Config;
import com.iso2t.sverve.survival.environment.HeatSourceConfig;
import com.iso2t.sverve.survival.exertion.LoadExertionConfig;
import com.iso2t.sverve.survival.moisture.MoistureConfig;
import com.iso2t.sverve.survival.temperature.TemperatureConfig;
import com.iso2t.sverve.survival.thirst.ThirstConfig;
import lombok.Getter;

/**
 * Easy Config schema. Each feature owns its section and default balance.
 */
@Getter
@Config(name = "sverve-survival", side = Side.SERVER)
public final class SurvivalConfig {
	@Comment("Optional Heavy Inventories integration: extra water loss from carrying weight while moving.")
	private final LoadExertionConfig heavyInventories = new LoadExertionConfig();
	@Comment("Body temperature and exposure to heat and cold.")
	private final TemperatureConfig  temperature      = new TemperatureConfig();

	@Comment("Getting wet and drying out.")
	private final MoistureConfig moisture = new MoistureConfig();

	@Comment("Water loss from time, heat, dry air, and exertion.")
	private final ThirstConfig thirst = new ThirstConfig();

	@Comment("Nearby campfire, torch, and lava warmth. The strongest visible source applies.")
	private final HeatSourceConfig heatSources = new HeatSourceConfig();
}
