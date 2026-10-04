package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.player.SurvivalEligibility;
import com.iso2t.sverve.survival.temperature.TemperatureMetabolismSystem;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared integration of thirst, vanilla exhaustion, and food-based healing with saved body exposure.
 */
@RequiredArgsConstructor
public final class TemperatureMetabolism {
	@NonNull
	private final PlayerTemperature           players;
	@NonNull
	private final TemperatureMetabolismSystem system;

	public void bind (ServerPlayer player) {
		((TemperatureFoodAccess) player.getFoodData()).sverve$setTemperatureMetabolism(this);
	}

	public void tick (ServerPlayer player, TemperatureState state) {
		if (eligible(player)) {
			double exposure = state.getExposure();
			if (system.foodExhaustionPerSecond(exposure) > 0) {
				player.causeFoodExhaustion((float) (system.foodExhaustionPerSecond(exposure, TemperatureProtection.sample(player)) / 20));
			}
		}
	}

	public double thirstHeat (ServerPlayer player) {
		if (!eligible(player)) return 0;
		double exposure = players.get(player).getExposure();
		return system.thirstHeat(exposure) == 0 ? 0 : system.thirstHeat(exposure, TemperatureProtection.sample(player));
	}

	public int healingInterval (ServerPlayer player, int original) {
		if (!eligible(player)) return original;
		double exposure = players.get(player).getExposure();
		return system.healingInterval(original, exposure) == original ? original : system.healingInterval(original, exposure, TemperatureProtection.sample(player));
	}

	private static boolean eligible (ServerPlayer player) {
		return SurvivalEligibility.canUpdate(player.gameMode(), player.isAlive());
	}
}
