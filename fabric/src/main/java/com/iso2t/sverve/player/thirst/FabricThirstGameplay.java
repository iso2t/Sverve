package com.iso2t.sverve.player.thirst;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

@UtilityClass
public class FabricThirstGameplay {

	public static void register (ThirstGameplay gameplay) {
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(gameplay::tick));
		FabricItemConsumptionEvents.FINISHED.register(gameplay::consume);
	}
}
