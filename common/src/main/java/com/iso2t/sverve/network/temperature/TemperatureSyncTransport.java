package com.iso2t.sverve.network.temperature;

import net.minecraft.server.level.ServerPlayer;

public interface TemperatureSyncTransport {

	TemperatureSyncTracker getTracker (ServerPlayer player);

	boolean send (ServerPlayer player, TemperatureSyncPayload payload);
}
