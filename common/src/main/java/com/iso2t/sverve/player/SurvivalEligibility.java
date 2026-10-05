package com.iso2t.sverve.player;

import lombok.experimental.UtilityClass;
import net.minecraft.world.level.GameType;

@UtilityClass
public class SurvivalEligibility {

	public static boolean canUpdate (GameType gameMode, boolean alive) {
		return alive && (gameMode == GameType.SURVIVAL || gameMode == GameType.ADVENTURE);
	}
}
