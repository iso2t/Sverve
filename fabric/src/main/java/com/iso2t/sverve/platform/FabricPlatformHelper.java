package com.iso2t.sverve.platform;

import com.iso2t.sverve.mixin.BiomeClimateAccessor;
import com.iso2t.sverve.platform.services.PlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.biome.Biome;

public class FabricPlatformHelper implements PlatformHelper {

	@Override
	public double getBiomeDownfall (Biome biome) {
		return ((BiomeClimateAccessor) (Object) biome).sverve$getClimateSettings().downfall();
	}

	@Override
	public String getPlatformName () {
		return "Fabric";
	}

	@Override
	public boolean isModLoaded (String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public boolean isDevelopmentEnvironment () {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}
}
