package com.iso2t.sverve.network.moisture;

import com.iso2t.sverve.Constants;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class FabricMoistureSyncTransport implements MoistureSyncTransport {
	private final AttachmentType<MoistureSyncTracker> tracker = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "moisture_sync"), builder -> builder.initializer(MoistureSyncTracker::new));

	public FabricMoistureSyncTransport () {
		PayloadTypeRegistry.clientboundPlay().register(MoistureSyncPayload.TYPE, MoistureSyncPayload.CODEC);
	}

	@Override
	public MoistureSyncTracker tracker (ServerPlayer player) {
		return player.getAttachedOrCreate(tracker);
	}

	@Override
	public boolean send (ServerPlayer player, MoistureSyncPayload payload) {
		if (!ServerPlayNetworking.canSend(player, MoistureSyncPayload.TYPE)) return false;
		ServerPlayNetworking.send(player, payload);
		return true;
	}
}
