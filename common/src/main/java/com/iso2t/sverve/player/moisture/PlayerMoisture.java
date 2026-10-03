package com.iso2t.sverve.player.moisture;

import com.iso2t.sverve.player.SurvivalEligibility;
import com.iso2t.sverve.survival.moisture.MoistureState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.UnaryOperator;

@RequiredArgsConstructor
public final class PlayerMoisture {
	@NonNull
	private final MoistureStorage storage;

	public MoistureState get (@NonNull ServerPlayer player) {
		return storage.get(player);
	}

	public MoistureState update (@NonNull ServerPlayer player, @NonNull UnaryOperator<MoistureState> change) {
		var current = get(player);
		if (!SurvivalEligibility.canUpdate(player.gameMode(), player.isAlive())) return current;
		var next = Objects.requireNonNull(change.apply(current), "Moisture update must return a state");
		if (!next.equals(current)) storage.set(player, next);
		return next;
	}
}
