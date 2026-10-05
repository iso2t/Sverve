package com.iso2t.sverve.client;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class SurvivalHudVisibility {

	public static boolean canShow (boolean enabled) {
		var client = Minecraft.getInstance();
		var player = client.player;
		return enabled && player != null && player.isAlive() && client.gameMode != null && client.gameMode.canHurtPlayer() && !client.gui.hud.isHidden() && client.getCameraEntity() == player;
	}
}
