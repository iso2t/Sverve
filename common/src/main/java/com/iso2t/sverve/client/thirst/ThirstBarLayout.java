package com.iso2t.sverve.client.thirst;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ThirstBarLayout {

	public static final int ICON_COUNT = 10;
	public static final int ICON_SIZE  = 9;

	public static int iconX (int guiWidth, int index) {
		return guiWidth / 2 + 91 - index * 8 - ICON_SIZE;
	}
}
