package com.iso2t.sverve.network.temperature;

import com.iso2t.sverve.Constants;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class FabricTemperatureSyncTransport implements TemperatureSyncTransport {
	private final AttachmentType<TemperatureSyncTracker> tracker = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "temperature_sync"), builder -> builder.initializer(TemperatureSyncTracker::new));

	public FabricTemperatureSyncTransport () {
		PayloadTypeRegistry.clientboundPlay().register(TemperatureSyncPayload.TYPE, TemperatureSyncPayload.CODEC);
	}

	@Override
	public TemperatureSyncTracker tracker (ServerPlayer player) {
		return player.getAttachedOrCreate(tracker);
	}

	@Override
	public boolean send (ServerPlayer player, TemperatureSyncPayload payload) {
		if (!ServerPlayNetworking.canSend(player, TemperatureSyncPayload.TYPE)) return false;
		ServerPlayNetworking.send(player, payload);
		return true;
	}
}
