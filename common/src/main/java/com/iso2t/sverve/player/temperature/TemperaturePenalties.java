package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.player.SurvivalEligibility;
import com.iso2t.sverve.survival.temperature.TemperatureBand;
import com.iso2t.sverve.config.TemperatureConfig;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

@RequiredArgsConstructor
public final class TemperaturePenalties {

	@NonNull
	private final TemperatureStorage storage;
	@NonNull
	private final TemperatureConfig  config;

	public void tick (ServerPlayer player, TemperatureState state) {
		var timer = storage.getDamageTimer(player);
		if (!config.getEnabled().get() || !SurvivalEligibility.canUpdate(player.gameMode(), player.isAlive())) {
			timer.reset();
			return;
		}
		var band = TemperatureBand.classify(state.exposure());

		if (band == TemperatureBand.HOT && player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
			timer.reset();
			return;
		}
		var damage = damageAmount(player, band);
		if (damage <= 0) {
			timer.reset();
			return;
		}
		if (timer.advance(band, config.getDamageIntervalSeconds().get() * 20)) {
			player.hurtServer(player.level(), TemperatureDamage.source(player, band), (float) damage);
		}
	}

	private double damageAmount (ServerPlayer player, TemperatureBand band) {
		var baseDamage = switch (band) {
			case FREEZING -> config.getFreezingDamage().get();
			case HOT -> config.getOverheatingDamage().get();
			default -> 0;
		};
		if (baseDamage <= 0) return 0;
		var protection = TemperatureProtection.sample(player);
		var reduction = band == TemperatureBand.FREEZING ? protection.coldReduction() : protection.heatReduction();
		return baseDamage * (1 - reduction);
	}
}
