package com.iso2t.sverve.network.moisture;

import com.iso2t.sverve.player.moisture.PlayerMoisture;
import com.iso2t.sverve.survival.moisture.MoistureConfig;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends visible wetness changes only to their owner, with fresh lifecycle snapshots.
 */
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
		var tracker = transport.tracker(player);
		var snapshot = MoistureSnapshot.from(players.get(player), config.getEnabled().get());
		if ((force || tracker.needsUpdate(snapshot)) && transport.send(player, new MoistureSyncPayload(snapshot))) {
			tracker.markSent(snapshot);
		}
	}
}
