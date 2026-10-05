package com.iso2t.sverve.mixin;

import com.iso2t.sverve.item.WaterBottles;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInstance.class)
public interface ItemInstanceMixin {

	@ModifyReturnValue(method = "getMaxStackSize", at = @At("RETURN"))
	private int sverve$waterStackSize (int original) {
		return WaterBottles.maxStackSize((ItemInstance) this, original);
	}
}
