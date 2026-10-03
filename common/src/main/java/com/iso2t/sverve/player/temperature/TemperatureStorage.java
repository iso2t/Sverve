package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.survival.temperature.TemperatureState;
import net.minecraft.server.level.ServerPlayer;

/**
 * Native storage for saved body exposure and a separate transient sampling clock.
 */
public interface TemperatureStorage {
	TemperatureState get (ServerPlayer player);

	void set (ServerPlayer player, TemperatureState state);

	TemperatureUpdateClock clock (ServerPlayer player);

	TemperatureDamageTimer damageTimer (ServerPlayer player);
}
