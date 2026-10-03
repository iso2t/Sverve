package com.iso2t.sverve.player.moisture;

import com.iso2t.sverve.network.moisture.MoistureSynchronizer;
import com.iso2t.sverve.player.SurvivalEligibility;
import com.iso2t.sverve.survival.environment.EnvironmentSample;
import com.iso2t.sverve.survival.moisture.MoistureConfig;
import com.iso2t.sverve.survival.moisture.MoistureSystem;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

/**
 * Schedules soaking/drying; the climate coordinator samples the world once when either feature is due.
 */
@RequiredArgsConstructor
public final class MoistureGameplay {
	@NonNull
	private final PlayerMoisture       players;
	@NonNull
	private final MoistureStorage      storage;
	@NonNull
	private final MoistureConfig       config;
	@NonNull
	private final MoistureSystem       system;
	@NonNull
	private final MoistureSynchronizer synchronizer;

	public boolean ready (ServerPlayer player) {
		if (!config.getEnabled().get() || !SurvivalEligibility.canUpdate(player.gameMode(), player.isAlive())) {
			pause(player);
			return false;
		}
		return storage.clock(player).advance();
	}

	public void advance (ServerPlayer player, EnvironmentSample sample) {
		players.update(player, state -> system.advance(state, sample, 1));
	}

	public double coolingWetness (ServerPlayer player) {
		return system.coolingWetness(players.get(player));
	}

	public void initialize (ServerPlayer player) {
		players.get(player);
		pause(player);
		synchronizer.refresh(player);
	}

	public void synchronize (ServerPlayer player) {
		synchronizer.update(player);
	}

	public void pause (ServerPlayer player) {
		storage.clock(player).reset();
	}
}
