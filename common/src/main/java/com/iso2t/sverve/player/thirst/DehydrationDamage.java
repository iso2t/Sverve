package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.Constants;
import lombok.experimental.UtilityClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

@UtilityClass
public class DehydrationDamage {

	public static final ResourceKey<DamageType> TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "dehydration"));

	public static DamageSource source (ServerPlayer player) {
		return new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(TYPE));
	}
}
