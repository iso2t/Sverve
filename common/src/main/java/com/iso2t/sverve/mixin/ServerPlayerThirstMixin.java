package com.iso2t.sverve.mixin;

import com.iso2t.sverve.player.thirst.ThirstGameplay;
import com.iso2t.sverve.player.thirst.ThirstMovementAccess;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerThirstMixin implements ThirstMovementAccess {

	@Unique
	private ThirstGameplay sverve$thirst;

	@Override
	public void sverve$bindThirst (ThirstGameplay gameplay) {
		sverve$thirst = gameplay;
	}

	@Inject(method = "checkMovementStatistics", at = @At("TAIL"))
	private void sverve$movementThirst (double dx, double dy, double dz, CallbackInfo callback) {
		if (sverve$thirst != null) sverve$thirst.moved((ServerPlayer) (Object) this, dx, dy, dz);
	}
}
