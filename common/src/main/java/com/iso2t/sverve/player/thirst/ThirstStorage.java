package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.survival.thirst.ThirstState;
import net.minecraft.server.level.ServerPlayer;

public interface ThirstStorage {

	ThirstState get (ServerPlayer player);

	void set (ServerPlayer player, ThirstState state);

	DehydrationTimer getDamageTimer (ServerPlayer player);
}
