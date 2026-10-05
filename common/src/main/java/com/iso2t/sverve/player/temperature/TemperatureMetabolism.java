package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.player.SurvivalEligibility;
import com.iso2t.sverve.survival.temperature.TemperatureMetabolismSystem;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

@RequiredArgsConstructor
public final class TemperatureMetabolism {

	@NonNull
	private final PlayerTemperature           players;
	@NonNull
	private final TemperatureMetabolismSystem system;

	public void bind (ServerPlayer player) {
		((TemperatureFoodAccess) player.getFoodData()).sverve$bindMetabolism(this);
	}

	public void tick (ServerPlayer player, TemperatureState state) {
		if (!eligible(player)) return;
		var exposure = state.exposure();
		if (system.foodExhaustion(exposure) > 0) {
			double exhaustion = system.foodExhaustion(exposure, TemperatureProtection.sample(player));
			player.causeFoodExhaustion((float) (exhaustion / 20));
		}
	}

	public double thirstHeat (ServerPlayer player) {
		if (!eligible(player)) return 0;
		var exposure = players.get(player).exposure();
		if (system.thirstHeat(exposure) == 0) return 0;
		return system.thirstHeat(exposure, TemperatureProtection.sample(player));
	}

	public int healingInterval (ServerPlayer player, int original) {
		if (!eligible(player)) return original;
		var exposure = players.get(player).exposure();
		if (system.healingInterval(original, exposure) == original) return original;
		return system.healingInterval(original, exposure, TemperatureProtection.sample(player));
	}

	private static boolean eligible (ServerPlayer player) {
		return SurvivalEligibility.canUpdate(player.gameMode(), player.isAlive());
	}
}
