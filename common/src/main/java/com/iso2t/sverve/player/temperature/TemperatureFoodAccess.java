package com.iso2t.sverve.player.temperature;

/**
 * A player's vanilla FoodData holds its injected service; no global runtime or player map.
 */
public interface TemperatureFoodAccess {
	void sverve$setTemperatureMetabolism (TemperatureMetabolism metabolism);
}
