package com.iso2t.sverve.player.thirst;

import lombok.experimental.UtilityClass;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@UtilityClass
public class NeoForgeThirstGameplay {

	public static void register (ThirstGameplay gameplay) {
		NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
			if (event.getEntity() instanceof ServerPlayer player) gameplay.tick(player);
		});
		NeoForge.EVENT_BUS.addListener((LivingEntityUseItemEvent.Finish event) -> {
			if (event.getEntity() instanceof ServerPlayer player) gameplay.consume(player, event.getItem());
		});
	}
}
