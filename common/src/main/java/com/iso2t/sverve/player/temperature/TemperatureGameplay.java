package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.network.temperature.TemperatureSynchronizer;
import com.iso2t.sverve.player.SurvivalEligibility;
import com.iso2t.sverve.player.environment.EnvironmentSampler;
import com.iso2t.sverve.player.moisture.MoistureGameplay;
import com.iso2t.sverve.config.TemperatureConfig;
import com.iso2t.sverve.survival.temperature.TemperatureSystem;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

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
	private final EnvironmentSampler      environment;
	@NonNull
	private final TemperatureSynchronizer synchronizer;
	@NonNull
	private final TemperaturePenalties    penalties;
	@NonNull
	private final TemperatureMetabolism   metabolism;
	@NonNull
	private final MoistureGameplay        moisture;

	public void tick (@NonNull ServerPlayer player) {
		if (!SurvivalEligibility.canUpdate(player.gameMode(), player.isAlive())) {
			pause(player);
			return;
		}
		updateExposure(player);
		applyEffects(player);
	}

	private void pause (ServerPlayer player) {
		storage.getClock(player).reset();
		moisture.pause(player);
		moisture.synchronize(player);
		penalties.tick(player, players.get(player));
		synchronizer.update(player);
	}

	private void updateExposure (ServerPlayer player) {
		var clock = storage.getClock(player);
		var temperatureDue = config.getEnabled().get() && clock.advance();
		if (!config.getEnabled().get()) clock.reset();
		var moistureDue = moisture.ready(player);
		if (temperatureDue || moistureDue) {
			var sample = environment.sample(player);
			if (moistureDue) moisture.advance(player, sample);
			if (temperatureDue) {
				var protection = TemperatureProtection.sample(player);
				players.update(player, state -> system.advance(state, sample, moisture.coolingWetness(player), protection, 1));
			}
		}
	}

	private void applyEffects (ServerPlayer player) {
		var state = players.get(player);
		moisture.synchronize(player);
		metabolism.tick(player, state);
		penalties.tick(player, state);
		synchronizer.update(player);
	}

	public void initialize (@NonNull ServerPlayer player) {
		players.get(player);
		moisture.initialize(player);
		metabolism.bind(player);
		storage.getClock(player).reset();
		storage.getDamageTimer(player).reset();
		synchronizer.refresh(player);
	}
}
