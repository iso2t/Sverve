package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.player.SurvivalEligibility;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Server-side body temperature access; load/save remain active for exempt players.
 */
@RequiredArgsConstructor
public final class PlayerTemperature {
	@NonNull
	private final TemperatureStorage storage;

	public TemperatureState get (@NonNull ServerPlayer player) {
		return storage.get(player);
	}

	public TemperatureState update (@NonNull ServerPlayer player, @NonNull UnaryOperator<TemperatureState> change) {
		TemperatureState current = get(player);
		if (!SurvivalEligibility.canUpdate(player.gameMode(), player.isAlive())) return current;
		TemperatureState next = Objects.requireNonNull(change.apply(current), "Temperature update must return a state");
		if (!next.equals(current)) storage.set(player, next);
		return next;
	}
}
