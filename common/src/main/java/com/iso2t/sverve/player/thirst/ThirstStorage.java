package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.survival.thirst.ThirstState;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-owned thirst state and transient damage timing. Access only from the server thread.
 */
public interface ThirstStorage {
	/**
	 * Returns saved thirst, creating a fully hydrated state when absent.
	 */
	ThirstState get (ServerPlayer player);

	void set (ServerPlayer player, ThirstState state);

	DehydrationTimer damageTimer (ServerPlayer player);
}
