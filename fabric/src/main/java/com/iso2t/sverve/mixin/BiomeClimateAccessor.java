package com.iso2t.sverve.mixin;

import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Biome.class)
public interface BiomeClimateAccessor {
	@Accessor("climateSettings")
	Biome.ClimateSettings sverve$getClimateSettings ();
}
