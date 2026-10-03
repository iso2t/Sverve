package com.iso2t.sverve.client;

import com.iso2t.sverve.client.temperature.TemperatureIconLayout;
import lombok.experimental.UtilityClass;

/**
 * Small right-side indicators leave space for vanilla's offhand and attack indicator when needed.
 */
@UtilityClass
public class HotbarIconLayout {
	public static int rightIconX (int screenWidth, boolean rightAccessory) {
		return TemperatureIconLayout.iconX(screenWidth) + (TemperatureIconLayout.ICON_SIZE / 2) + 5;/*screenWidth / 2 + 81 + 6 + (rightAccessory ? 29 : 0);*/
	}

	public static int iconY (int screenHeight, int iconSize) {
		return TemperatureIconLayout.iconY(screenHeight) + TemperatureIconLayout.ICON_SIZE - (iconSize / 2) - 1 /*+ (16 - iconSize) / 2*/;
	}
}
