package com.iso2t.sverve.player.environment;

import com.iso2t.sverve.platform.Services;
import com.iso2t.sverve.survival.Mth;
import com.iso2t.sverve.survival.environment.EnvironmentSample;
import com.iso2t.sverve.survival.temperature.BiomeTemperatureMapping;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

@RequiredArgsConstructor
public final class EnvironmentSampler {

	@NonNull
	private final BiomeTemperatureMapping temperatures;
	@NonNull
	private final NearbyHeatSampler       heat;

	public EnvironmentSample sample (@NonNull ServerPlayer player) {
		var biome = player.level().getBiome(player.blockPosition()).value();
		return EnvironmentSample.builder().ambientTemperature(temperatures.map(biome.getBaseTemperature())).nearbyHeat(heat.sample(player)).humidity(humidity(biome)).exposedToRain(player.level().isRainingAt(player.blockPosition())).immersed(player.isInWater()).build();
	}

	public double humidity (@NonNull ServerPlayer player) {
		return humidity(player.level().getBiome(player.blockPosition()).value());
	}

	private static double humidity (Biome biome) {
		var downfall = Services.PLATFORM.getBiomeDownfall(biome);
		return Double.isFinite(downfall) ? Mth.clamp(downfall, 0.0, 1.0) : 0.5;
	}
}
