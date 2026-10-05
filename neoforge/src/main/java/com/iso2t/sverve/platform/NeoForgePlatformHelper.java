package com.iso2t.sverve.platform;

import com.iso2t.sverve.platform.services.PlatformHelper;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

public class NeoForgePlatformHelper implements PlatformHelper {

	@Override
	public double getBiomeDownfall (Biome biome) {
		return biome.getModifiedClimateSettings().downfall();
	}

	@Override
	public String getPlatformName () {
		return "NeoForge";
	}

	@Override
	public boolean isModLoaded (String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public boolean isDevelopmentEnvironment () {
		return !FMLLoader.getCurrent().isProduction();
	}
}
