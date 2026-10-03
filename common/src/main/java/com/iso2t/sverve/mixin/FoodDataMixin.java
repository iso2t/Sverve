package com.iso2t.sverve.mixin;

import com.iso2t.sverve.player.temperature.TemperatureFoodAccess;
import com.iso2t.sverve.player.temperature.TemperatureMetabolism;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Changes only natural regeneration cadence; starvation's separate 80-tick threshold stays vanilla.
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin implements TemperatureFoodAccess {
	@Unique
	private TemperatureMetabolism sverve$metabolism;

	@Override
	public void sverve$setTemperatureMetabolism (TemperatureMetabolism metabolism) {
		sverve$metabolism = metabolism;
	}

	@ModifyExpressionValue(method = "tick", at = @At(value = "CONSTANT", args = "intValue=10"), require = 1)
	private int sverve$saturatedHealingInterval (int original, ServerPlayer player) {
		return sverve$metabolism == null ? original : sverve$metabolism.healingInterval(player, original);
	}

	@ModifyExpressionValue(method = "tick", at = @At(value = "CONSTANT", args = "intValue=80", ordinal = 0), require = 1)
	private int sverve$normalHealingInterval (int original, ServerPlayer player) {
		return sverve$metabolism == null ? original : sverve$metabolism.healingInterval(player, original);
	}
}
