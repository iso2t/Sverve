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
			player.causeFoodExhaustion((float) (system.foodExhaustionPerSecond(state.getExposure()) / 20));
		}
	}

	public double thirstMultiplier (ServerPlayer player) {
		return eligible(player) ? system.thirstMultiplier(players.get(player).getExposure()) : 1;
	}

	public int healingInterval (ServerPlayer player, int original) {
		return eligible(player) ? system.healingInterval(original, players.get(player).getExposure()) : original;
	}

	private static boolean eligible (ServerPlayer player) {
		return SurvivalEligibility.canUpdate(player.gameMode(), player.isAlive());
	}
}
