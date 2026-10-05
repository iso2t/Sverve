package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.network.thirst.ThirstSynchronizer;
import lombok.experimental.UtilityClass;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@UtilityClass
public class NeoForgeThirstLifecycle {

	public static void register (ThirstSynchronizer synchronizer) {
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) synchronizer.refresh(player);
		});
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) synchronizer.refresh(player);
		});
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) synchronizer.refresh(player);
		});
	}
}
