package com.iso2t.sverve.network.moisture;

import com.iso2t.sverve.Constants;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class NeoForgeMoistureSyncTransport implements MoistureSyncTransport {

	private final DeferredRegister<AttachmentType<?>>           attachments = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MOD_ID);
	private final Supplier<AttachmentType<MoistureSyncTracker>> tracker     = attachments.register("moisture_sync", () -> AttachmentType.builder(MoistureSyncTracker::new).build());

	public void register (IEventBus modBus) {
		attachments.register(modBus);
		modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").playToClient(MoistureSyncPayload.TYPE, MoistureSyncPayload.CODEC));
	}

	@Override
	public MoistureSyncTracker getTracker (ServerPlayer player) {
		return player.getData(tracker);
	}

	@Override
	public boolean send (ServerPlayer player, MoistureSyncPayload payload) {
		if (!player.connection.hasChannel(MoistureSyncPayload.TYPE)) return false;
		PacketDistributor.sendToPlayer(player, payload);
		return true;
	}
}
