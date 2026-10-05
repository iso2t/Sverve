package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.survival.thirst.ThirstState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class FabricThirstStorage implements ThirstStorage {

	private final AttachmentType<ThirstState> thirst = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "thirst"), builder -> builder.initializer(ThirstState::hydrated).persistent(ThirstPersistence.CODEC.codec()));

	private final AttachmentType<DehydrationTimer> damageTimer = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "dehydration_timer"), builder -> builder.initializer(DehydrationTimer::new));

	@Override
	public DehydrationTimer getDamageTimer (ServerPlayer player) {
		return player.getAttachedOrCreate(damageTimer);
	}

	@Override
	public ThirstState get (ServerPlayer player) {
		return player.getAttachedOrCreate(thirst);
	}

	@Override
	public void set (ServerPlayer player, ThirstState state) {
		player.setAttached(thirst, state);
	}
}
