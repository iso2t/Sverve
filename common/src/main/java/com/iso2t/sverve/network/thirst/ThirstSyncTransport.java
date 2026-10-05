package com.iso2t.sverve.network.thirst;

import net.minecraft.server.level.ServerPlayer;

public interface ThirstSyncTransport {

	ThirstSyncTracker getTracker (ServerPlayer player);

	boolean send (ServerPlayer player, ThirstSyncPayload payload);
}
