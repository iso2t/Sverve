package com.iso2t.sverve.player.moisture;

import com.iso2t.sverve.survival.moisture.MoistureState;
import net.minecraft.server.level.ServerPlayer;

public interface MoistureStorage {

	MoistureState get (ServerPlayer player);

	void set (ServerPlayer player, MoistureState state);

	MoistureUpdateClock getClock (ServerPlayer player);
}
