package com.iso2t.sverve.mixin;

import com.iso2t.sverve.player.thirst.FabricItemConsumptionEvents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityConsumptionMixin {

	@WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack sverve$afterItemUse (ItemStack stack, Level level, LivingEntity user, Operation<ItemStack> original) {
		if (!(user instanceof ServerPlayer player)) return original.call(stack, level, user);

		ItemStack consumed = stack.copy();
		ItemStack result = original.call(stack, level, user);
		FabricItemConsumptionEvents.FINISHED.invoker().onFinished(player, consumed);
		return result;
	}
}
