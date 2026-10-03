package com.iso2t.sverve.network.temperature;

import net.minecraft.server.level.ServerPlayer;

/**
 * Native transient owner tracking and delivery; false means the channel is not ready.
 */
public interface TemperatureSyncTransport {
	TemperatureSyncTracker tracker (ServerPlayer player);

	boolean send (ServerPlayer player, TemperatureSyncPayload payload);
}
