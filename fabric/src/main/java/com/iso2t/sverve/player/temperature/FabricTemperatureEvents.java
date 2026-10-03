package com.iso2t.sverve.player.temperature;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Native lifecycle and tick bridges; temperature calculations stay in common.
 */
@UtilityClass
public class FabricTemperatureEvents {
	public static void register (TemperatureGameplay gameplay) {
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(gameplay::tick));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> gameplay.initialize(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> gameplay.initialize(newPlayer));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> gameplay.initialize(player));
	}
}
