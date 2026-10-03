package com.iso2t.sverve.network.moisture;

import net.minecraft.server.level.ServerPlayer;

/**
 * Native owner tracking and delivery; false means the channel is not ready.
 */
public interface MoistureSyncTransport {
	MoistureSyncTracker tracker (ServerPlayer player);

	boolean send (ServerPlayer player, MoistureSyncPayload payload);
}
