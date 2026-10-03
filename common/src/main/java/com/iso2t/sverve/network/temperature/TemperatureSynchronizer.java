package com.iso2t.sverve.network.temperature;

import com.iso2t.sverve.player.temperature.PlayerTemperature;
import com.iso2t.sverve.survival.temperature.TemperatureConfig;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends stable display changes to their owner and refreshes after lifecycle transitions.
 */
@RequiredArgsConstructor
public final class TemperatureSynchronizer {
	@NonNull
	private final PlayerTemperature        players;
	@NonNull
	private final TemperatureConfig        config;
	@NonNull
	private final TemperatureSyncTransport transport;

	public void update (@NonNull ServerPlayer player) {
		publish(player, false);
	}

	public void refresh (@NonNull ServerPlayer player) {
		publish(player, true);
	}

	private void publish (ServerPlayer player, boolean force) {
		var tracker = transport.tracker(player);
		var snapshot = tracker.snapshot(players.get(player).getExposure(), config.getEnabled().get(), force);
		if ((force || tracker.needsUpdate(snapshot)) && transport.send(player, new TemperatureSyncPayload(snapshot))) {
			tracker.markSent(snapshot);
		}
	}
}
