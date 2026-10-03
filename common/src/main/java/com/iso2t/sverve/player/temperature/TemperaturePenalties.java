package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.player.SurvivalEligibility;
import com.iso2t.sverve.survival.temperature.TemperatureBand;
import com.iso2t.sverve.survival.temperature.TemperatureConfig;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

/**
 * Extreme body thresholds govern damage; the HUD's hysteresis never changes gameplay.
 */
@RequiredArgsConstructor
public final class TemperaturePenalties {
	@NonNull
	private final TemperatureStorage storage;
	@NonNull
	private final TemperatureConfig  config;

	public void tick (ServerPlayer player, TemperatureState state) {
		var timer = storage.damageTimer(player);
		if (!config.getEnabled().get() || !SurvivalEligibility.canUpdate(player.gameMode(), player.isAlive())) {
			timer.reset();
			return;
		}
		var band = TemperatureBand.classify(state.getExposure());
		double baseDamage = switch (band) {
			case FREEZING -> config.getFreezingDamage().get();
			case HOT -> config.getOverheatingDamage().get();
			default -> 0;
		};
		if (baseDamage <= 0) {
			timer.reset();
			return;
		}
		var protection = TemperatureProtection.sample(player);
		double reduction = band == TemperatureBand.FREEZING ? protection.getColdReduction() : protection.getHeatReduction();
		if (reduction >= 1) {
			timer.reset();
			return;
		}
		if (timer.advance(band, config.getDamageIntervalSeconds().get() * 20)) {
			player.hurtServer(player.level(), TemperatureDamage.source(player, band), (float) (baseDamage * (1 - reduction)));
		}
	}
}
