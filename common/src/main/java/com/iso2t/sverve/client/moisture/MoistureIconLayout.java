package com.iso2t.sverve.client.moisture;

import com.iso2t.sverve.client.temperature.TemperatureIconLayout;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MoistureIconLayout {

	public static int x (int screenWidth) {
		return TemperatureIconLayout.iconX(screenWidth) + TemperatureIconLayout.ICON_SIZE / 2 + 5;
	}

	public static int y (int screenHeight, int iconSize) {
		return TemperatureIconLayout.iconY(screenHeight) + TemperatureIconLayout.ICON_SIZE - iconSize / 2 - 1;
	}
}
