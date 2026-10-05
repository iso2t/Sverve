package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.survival.temperature.TemperatureState;
import net.minecraft.server.level.ServerPlayer;

public interface TemperatureStorage {

	TemperatureState get (ServerPlayer player);

	void set (ServerPlayer player, TemperatureState state);

	TemperatureUpdateClock getClock (ServerPlayer player);

	TemperatureDamageTimer getDamageTimer (ServerPlayer player);
}
