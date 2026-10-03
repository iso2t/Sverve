package com.iso2t.sverve.player.moisture;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.survival.moisture.MoistureState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class NeoForgeMoistureStorage implements MoistureStorage {
	private final DeferredRegister<AttachmentType<?>>           attachments = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MOD_ID);
	private final Supplier<AttachmentType<MoistureState>>       moisture    = attachments.register("moisture", () -> AttachmentType.builder(MoistureState::dry).serialize(MoisturePersistence.CODEC).build());
	// No copyOnDeath: death respawn starts dry regardless of keepInventory.
	private final Supplier<AttachmentType<MoistureUpdateClock>> clock       = attachments.register("moisture_clock", () -> AttachmentType.builder(MoistureUpdateClock::new).build());

	public void register (IEventBus modBus) {
		attachments.register(modBus);
	}

	@Override
	public MoistureState get (ServerPlayer player) {
		return player.getData(moisture);
	}

	@Override
	public void set (ServerPlayer player, MoistureState state) {
		player.setData(moisture, state);
	}

	@Override
	public MoistureUpdateClock clock (ServerPlayer player) {
		return player.getData(clock);
	}
}
