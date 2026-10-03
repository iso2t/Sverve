package com.iso2t.sverve.network.thirst;

import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-owned transient attachment access and delivery to one connection.
 */
public interface ThirstSyncTransport {
	ThirstSyncTracker tracker (ServerPlayer player);

	/**
	 * Returns false when the connection is not ready to receive this payload.
	 */
	boolean send (ServerPlayer player, ThirstSyncPayload payload);
}
