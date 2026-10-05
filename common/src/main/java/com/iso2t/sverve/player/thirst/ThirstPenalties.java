package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.config.ThirstConfig;
import com.iso2t.sverve.survival.thirst.ThirstState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

@RequiredArgsConstructor
public final class ThirstPenalties {

	private static final int TICKS_PER_SECOND = 20;

	@NonNull
	private final ThirstStorage storage;
	@NonNull
	private final ThirstConfig  config;

	public void tick (ServerPlayer player, ThirstState state) {
		var timer = storage.getDamageTimer(player);
		if (!PlayerThirst.isEligible(player.gameMode(), player.isAlive()) || state.hydration() > 0.0) {
			timer.reset();
			return;
		}
		if (!config.getEnabled().get() || config.getDehydrationDamage().get() == 0.0) return;
		if (timer.advance(config.getDehydrationIntervalSeconds().get() * TICKS_PER_SECOND)) {
			player.hurtServer(player.level(), DehydrationDamage.source(player), config.getDehydrationDamage().get().floatValue());
		}
	}

	public void hydrationChanged (ServerPlayer player, ThirstState state) {
		if (state.hydration() > 0.0) storage.getDamageTimer(player).reset();
	}
}
