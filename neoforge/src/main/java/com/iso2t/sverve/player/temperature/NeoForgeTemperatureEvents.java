package com.iso2t.sverve.player.temperature;

import lombok.experimental.UtilityClass;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@UtilityClass
public class NeoForgeTemperatureEvents {

	public static void register (TemperatureGameplay gameplay) {
		NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
			if (event.getEntity() instanceof ServerPlayer player) gameplay.tick(player);
		});
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) gameplay.initialize(player);
		});
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) gameplay.initialize(player);
		});
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) gameplay.initialize(player);
		});
	}
}
