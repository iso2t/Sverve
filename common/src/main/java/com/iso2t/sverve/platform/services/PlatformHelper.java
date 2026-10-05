package com.iso2t.sverve.platform.services;

import net.minecraft.world.level.biome.Biome;

public interface PlatformHelper {

	double getBiomeDownfall (Biome biome);

	String getPlatformName ();

	boolean isModLoaded (String modId);

	boolean isDevelopmentEnvironment ();

	default String getEnvironmentName () {
		return isDevelopmentEnvironment() ? "development" : "production";
	}
}
