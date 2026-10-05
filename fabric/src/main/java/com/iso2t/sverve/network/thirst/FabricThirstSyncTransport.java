package com.iso2t.sverve.network.thirst;

import com.iso2t.sverve.Constants;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class FabricThirstSyncTransport implements ThirstSyncTransport {

	private final AttachmentType<ThirstSyncTracker> tracker = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "thirst_sync"), builder -> builder.initializer(ThirstSyncTracker::new));

	public FabricThirstSyncTransport () {
		PayloadTypeRegistry.clientboundPlay().register(ThirstSyncPayload.TYPE, ThirstSyncPayload.CODEC);
	}

	@Override
	public ThirstSyncTracker getTracker (ServerPlayer player) {
		return player.getAttachedOrCreate(tracker);
	}

	@Override
	public boolean send (ServerPlayer player, ThirstSyncPayload payload) {
		if (!ServerPlayNetworking.canSend(player, ThirstSyncPayload.TYPE)) return false;
		ServerPlayNetworking.send(player, payload);
		return true;
	}
}
