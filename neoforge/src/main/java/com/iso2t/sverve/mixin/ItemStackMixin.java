package com.iso2t.sverve.mixin;

import com.iso2t.sverve.item.WaterBottles;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge overrides ItemInstance's default stack limit on ItemStack.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@ModifyReturnValue(method = "getMaxStackSize", at = @At("RETURN"))
	private int sverve$waterBottleStackSize (int original) {
		return WaterBottles.maxStackSize((ItemStack) (Object) this, original);
	}
}
