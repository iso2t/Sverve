package com.iso2t.sverve.client.temperature;

import lombok.experimental.UtilityClass;

/**
 * Centers the head horizontally and aligns it with the middle of the food/thirst stack.
 */
@UtilityClass
public class TemperatureIconLayout {
	public static final int ICON_SIZE = 14;

	public static int iconX (int screenWidth) {
		return screenWidth / 2 - ICON_SIZE / 2;
	}

	public static int iconY (int screenHeight) {
		return screenHeight - 51;
	}
}
