package com.iso2t.sverve.network.moisture;

import com.iso2t.sverve.player.moisture.PlayerMoisture;
import com.iso2t.sverve.config.MoistureConfig;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

@RequiredArgsConstructor
public final class MoistureSynchronizer {

	@NonNull
	private final PlayerMoisture        players;
	@NonNull
	private final MoistureConfig        config;
	@NonNull
	private final MoistureSyncTransport transport;

	public void update (@NonNull ServerPlayer player) {
		publish(player, false);
	}

	public void refresh (@NonNull ServerPlayer player) {
		publish(player, true);
	}

	private void publish (ServerPlayer player, boolean force) {
		var tracker = transport.getTracker(player);
		var snapshot = MoistureSnapshot.from(players.get(player), config.getEnabled().get());
		if ((force || tracker.needsUpdate(snapshot)) && transport.send(player, new MoistureSyncPayload(snapshot))) {
			tracker.markSent(snapshot);
		}
	}
}
