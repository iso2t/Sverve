package com.iso2t.sverve.network.moisture;

import net.minecraft.server.level.ServerPlayer;

public interface MoistureSyncTransport {

	MoistureSyncTracker getTracker (ServerPlayer player);

	boolean send (ServerPlayer player, MoistureSyncPayload payload);
}
