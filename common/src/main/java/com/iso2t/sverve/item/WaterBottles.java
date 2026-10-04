package com.iso2t.sverve.item;

import lombok.experimental.UtilityClass;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Plain drinkable water keeps vanilla item use and stacks up to sixteen.
 */
@UtilityClass
public class WaterBottles {
	public static final int MAX_STACK_SIZE = 16;

	public static boolean isPlainWater (ItemInstance item) {
		return item.typeHolder().value() == Items.POTION
				&& item.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
	}

	public static int maxStackSize (ItemInstance item, int vanillaLimit) {
		return isPlainWater(item) ? MAX_STACK_SIZE : vanillaLimit;
	}
}
