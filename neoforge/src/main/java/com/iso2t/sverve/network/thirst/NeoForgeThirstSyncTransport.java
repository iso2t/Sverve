package com.iso2t.sverve.network.thirst;

import com.iso2t.sverve.Constants;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class NeoForgeThirstSyncTransport implements ThirstSyncTransport {
	private final DeferredRegister<AttachmentType<?>>         attachments = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MOD_ID);
	private final Supplier<AttachmentType<ThirstSyncTracker>> tracker     = attachments.register("thirst_sync", () -> AttachmentType.builder(ThirstSyncTracker::new).build());

	public void register (IEventBus modBus) {
		attachments.register(modBus);
		modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").playToClient(ThirstSyncPayload.TYPE, ThirstSyncPayload.CODEC));
	}

	@Override
	public ThirstSyncTracker tracker (ServerPlayer player) {
		return player.getData(tracker);
	}

	@Override
	public boolean send (ServerPlayer player, ThirstSyncPayload payload) {
		if (!player.connection.hasChannel(ThirstSyncPayload.TYPE)) return false;
		PacketDistributor.sendToPlayer(player, payload);
		return true;
	}
}
