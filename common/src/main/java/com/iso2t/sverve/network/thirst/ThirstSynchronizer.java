package com.iso2t.sverve.network.thirst;

import com.iso2t.sverve.player.thirst.PlayerThirst;
import com.iso2t.sverve.config.ThirstConfig;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

@RequiredArgsConstructor
public final class ThirstSynchronizer {

	@NonNull
	private final PlayerThirst        players;
	@NonNull
	private final ThirstConfig        config;
	@NonNull
	private final ThirstSyncTransport transport;

	public void update (@NonNull ServerPlayer player) {
		publish(player, false);
	}

	public void refresh (@NonNull ServerPlayer player) {
		publish(player, true);
	}

	private void publish (ServerPlayer player, boolean force) {
		var snapshot = ThirstSnapshot.from(players.get(player), config.getEnabled().get());
		var tracker = transport.getTracker(player);
		if ((force || tracker.needsUpdate(snapshot)) && transport.send(player, new ThirstSyncPayload(snapshot))) {
			tracker.markSent(snapshot);
		}
	}
}
