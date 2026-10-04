package com.iso2t.sverve.player.environment;

import com.iso2t.sverve.platform.Services;
import com.iso2t.sverve.survival.SurvivalMath;
import com.iso2t.sverve.survival.environment.EnvironmentSample;
import com.iso2t.sverve.survival.temperature.BiomeTemperatureMapping;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared world adapter. Reads the current registered biome, including datapack/modded entries.
 */
@RequiredArgsConstructor
public final class BiomeEnvironmentSampler {
	@NonNull
	private final BiomeTemperatureMapping temperatures;
	@NonNull
	private final NearbyHeatSampler       heat;

	public EnvironmentSample sample (@NonNull ServerPlayer player) {
		var biome = player.level().getBiome(player.blockPosition()).value();
		// Vanilla's rain check respects roofs, rainless biomes, and snow precipitation.
		return EnvironmentSample.builder().ambientTemperature(temperatures.map(biome.getBaseTemperature())).nearbyHeat(heat.sample(player)).humidity(humidity(player)).exposedToRain(player.level().isRainingAt(player.blockPosition())).immersed(player.isInWater()).sprinting(player.isSprinting()).build();
	}

	/**
	 * Cheap climate lookup for thirst; does not scan nearby heat sources.
	 */
	public double humidity (@NonNull ServerPlayer player) {
		double downfall = Services.PLATFORM.biomeDownfall(player.level().getBiome(player.blockPosition()).value());
		return Double.isFinite(downfall) ? SurvivalMath.clamp(downfall, 0.0, 1.0) : 0.5;
	}
}
