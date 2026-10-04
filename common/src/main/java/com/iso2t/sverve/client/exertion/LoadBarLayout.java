package com.iso2t.sverve.client.exertion;

import com.iso2t.sverve.client.temperature.TemperatureIconLayout;
import lombok.experimental.UtilityClass;

/**
 * An item-style load bar over the face's bottom two rows, left of the moisture droplet.
 */
@UtilityClass
public class LoadBarLayout {
	public static final int WIDTH          = TemperatureIconLayout.ICON_SIZE - 3;
	public static final int HEIGHT         = 2;
	public static final int INNER_WIDTH    = WIDTH - 2;
	public static final int EMPTY_COLOR    = 0xFF737B86;
	public static final int NORMAL_COLOR   = 0xFF78A84C;
	public static final int WARNING_COLOR  = 0xFFE3B341;
	public static final int OVERLOAD_COLOR = 0xFFD34A40;

	public static int x (int screenWidth) {
		return TemperatureIconLayout.iconX(screenWidth) + 1;
	}

	public static int y (int screenHeight) {
		return TemperatureIconLayout.iconY(screenHeight) + TemperatureIconLayout.ICON_SIZE - HEIGHT;
	}

	public static int fillWidth (double ratio) {
		if (Double.isNaN(ratio) || ratio <= 0.0) return 0;
		return Math.max(1, (int) Math.round(Math.min(ratio, 1.0) * INNER_WIDTH));
	}

	public static int color (double ratio) {
		return ratio >= 1.0 ? OVERLOAD_COLOR : ratio >= 0.9 ? WARNING_COLOR : NORMAL_COLOR;
	}
}
