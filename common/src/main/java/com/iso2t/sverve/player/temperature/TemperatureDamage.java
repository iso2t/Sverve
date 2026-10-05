package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.survival.temperature.TemperatureBand;
import lombok.experimental.UtilityClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

@UtilityClass
public class TemperatureDamage {

	public static final ResourceKey<DamageType> FREEZING    = key("freezing");
	public static final ResourceKey<DamageType> OVERHEATING = key("overheating");

	public static DamageSource source (ServerPlayer player, TemperatureBand band) {
		var key = switch (band) {
			case FREEZING -> FREEZING;
			case HOT -> OVERHEATING;
			default -> throw new IllegalArgumentException("Only extreme temperature deals damage");
		};
		return new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key));
	}

	private static ResourceKey<DamageType> key (String path) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, path));
	}
}
