package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.survival.temperature.TemperatureProtectionScore;
import lombok.experimental.UtilityClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Reads worn armor at the damage pulse; inventory and held items never contribute.
 */
@UtilityClass
public class TemperatureProtection {
	public static final  ResourceKey<Enchantment> INSULATION         = key("insulation");
	public static final  ResourceKey<Enchantment> HEAT_PROTECTION    = key("heat_protection");
	public static final  ResourceKey<Enchantment> THERMAL_PROTECTION = key("thermal_protection");
	private static final EquipmentSlot[]          ARMOR              = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

	public static TemperatureProtectionScore sample (ServerPlayer player) {
		return TemperatureProtectionScore.fromLevels(levels(player, INSULATION), levels(player, HEAT_PROTECTION), levels(player, THERMAL_PROTECTION));
	}

	private static int levels (ServerPlayer player, ResourceKey<Enchantment> key) {
		var enchantment = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
		int total = 0;
		for (var slot : ARMOR) {
			var piece = player.getItemBySlot(slot);
			if (piece.is(enchantment.value().getSupportedItems())) {
				// Commands can supply oversized levels; a piece still contributes at most IV.
				total += Math.min(4, EnchantmentHelper.getItemEnchantmentLevel(enchantment, piece));
			}
		}
		return total;
	}

	private static ResourceKey<Enchantment> key (String path) {
		return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Constants.MOD_ID, path));
	}
}
