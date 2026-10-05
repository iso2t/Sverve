package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.network.thirst.ThirstSynchronizer;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

@UtilityClass
public class FabricThirstLifecycle {

	public static void register (ThirstSynchronizer synchronizer) {
		ServerPlayConnectionEvents.JOIN.register((handler, _, server) -> synchronizer.refresh(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((_, newPlayer, _) -> synchronizer.refresh(newPlayer));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, _, _) -> synchronizer.refresh(player));
	}
}
