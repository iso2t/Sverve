package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.player.SurvivalEligibility;
import com.iso2t.sverve.survival.thirst.ThirstState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Objects;
import java.util.function.UnaryOperator;

@RequiredArgsConstructor
public final class PlayerThirst {

	@NonNull
	private final ThirstStorage storage;

	public ThirstState get (@NonNull ServerPlayer player) {
		return storage.get(player);
	}

	public ThirstState update (@NonNull ServerPlayer player, @NonNull UnaryOperator<ThirstState> change) {
		ThirstState current = get(player);
		if (!isEligible(player.gameMode(), player.isAlive())) return current;

		ThirstState next = Objects.requireNonNull(change.apply(current), "Thirst update must return a state");
		if (!next.equals(current)) storage.set(player, next);
		return next;
	}

	public static boolean isEligible (GameType gameMode, boolean alive) {
		return SurvivalEligibility.canUpdate(gameMode, alive);
	}
}
