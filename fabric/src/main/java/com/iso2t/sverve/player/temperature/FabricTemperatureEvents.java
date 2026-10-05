package com.iso2t.sverve.player.temperature;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

@UtilityClass
public class FabricTemperatureEvents {

	public static void register (TemperatureGameplay gameplay) {
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(gameplay::tick));
		ServerPlayConnectionEvents.JOIN.register((handler, _, _) -> gameplay.initialize(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((_, newPlayer, _) -> gameplay.initialize(newPlayer));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, _, _) -> gameplay.initialize(player));
	}
}
