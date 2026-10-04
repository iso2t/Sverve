package com.iso2t.sverve.mixin;

import com.iso2t.sverve.item.WaterBottles;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Reads the current contents so existing and newly filled water bottles share the same limit.
 */
@Mixin(ItemInstance.class)
public interface ItemInstanceMixin {
	@ModifyReturnValue(method = "getMaxStackSize", at = @At("RETURN"))
	private int sverve$waterBottleStackSize (int original) {
		return WaterBottles.maxStackSize((ItemInstance) this, original);
	}
}
