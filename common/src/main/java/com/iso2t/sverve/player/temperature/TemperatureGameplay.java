package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.network.temperature.TemperatureSynchronizer;
import com.iso2t.sverve.player.SurvivalEligibility;
import com.iso2t.sverve.player.environment.BiomeEnvironmentSampler;
import com.iso2t.sverve.player.moisture.MoistureGameplay;
import com.iso2t.sverve.survival.temperature.TemperatureConfig;
import com.iso2t.sverve.survival.temperature.TemperatureSystem;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

/**
 * Coordinates wetness and temperature samples with independent clocks.
 */
@RequiredArgsConstructor
public final class TemperatureGameplay {
	@NonNull
	private final PlayerTemperature       players;
	@NonNull
	private final TemperatureStorage      storage;
	@NonNull
	private final TemperatureConfig       config;
	@NonNull
	private final TemperatureSystem       system;
	@NonNull
	private final BiomeEnvironmentSampler environment;
	@NonNull
	private final TemperatureSynchronizer synchronizer;
	@NonNull
	private final TemperaturePenalties    penalties;
	@NonNull
	private final TemperatureMetabolism   metabolism;
	@NonNull
	private final MoistureGameplay        moisture;

	public void tick (@NonNull ServerPlayer player) {
		var clock = storage.clock(player);
		if (!SurvivalEligibility.canUpdate(player.gameMode(), player.isAlive())) {
			clock.reset();
			moisture.pause(player);
			moisture.synchronize(player);
			penalties.tick(player, players.get(player));
			synchronizer.update(player);
			return;
		}
		boolean temperatureDue = config.getEnabled().get() && clock.advance();
		if (!config.getEnabled().get()) clock.reset();
		boolean moistureDue = moisture.ready(player);
		if (temperatureDue || moistureDue) {
			var sample = environment.sample(player);
			if (moistureDue) moisture.advance(player, sample);
			if (temperatureDue) {
				players.update(player, state -> system.advance(state, sample, moisture.coolingWetness(player), 1));
			}
		}
		var state = players.get(player);
		moisture.synchronize(player);
		metabolism.tick(player, state);
		penalties.tick(player, state);
		synchronizer.update(player);
	}

	/**
	 * Loading/copying belongs to the loader; connection transitions start a fresh sampling interval.
	 */
	public void initialize (@NonNull ServerPlayer player) {
		players.get(player);
		moisture.initialize(player);
		metabolism.bind(player);
		storage.clock(player).reset();
		storage.damageTimer(player).reset();
		synchronizer.refresh(player);
	}
}
