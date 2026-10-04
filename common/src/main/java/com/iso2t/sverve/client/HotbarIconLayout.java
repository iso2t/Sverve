package com.iso2t.sverve.client;

import com.iso2t.sverve.client.temperature.TemperatureIconLayout;
import lombok.experimental.UtilityClass;

/**
 * Anchors the moisture indicator to the lower right of the temperature head.
 */
@UtilityClass
public class HotbarIconLayout {
	public static int rightIconX (int screenWidth, boolean rightAccessory) {
		return TemperatureIconLayout.iconX(screenWidth) + (TemperatureIconLayout.ICON_SIZE / 2) + 5;
	}

	public static int iconY (int screenHeight, int iconSize) {
		return TemperatureIconLayout.iconY(screenHeight) + TemperatureIconLayout.ICON_SIZE - (iconSize / 2) - 1;
	}
}
