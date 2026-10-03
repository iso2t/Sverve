package com.iso2t.sverve.player.moisture;

import com.iso2t.sverve.survival.moisture.MoistureState;
import net.minecraft.server.level.ServerPlayer;

/**
 * Native saved wetness and an independent transient sampling interval.
 */
public interface MoistureStorage {
	MoistureState get (ServerPlayer player);

	void set (ServerPlayer player, MoistureState state);

	MoistureUpdateClock clock (ServerPlayer player);
}
