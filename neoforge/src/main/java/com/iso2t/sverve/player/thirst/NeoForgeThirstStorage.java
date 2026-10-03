package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.survival.thirst.ThirstState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * NeoForge owns persistence and copying on non-death player replacement.
 */
public final class NeoForgeThirstStorage implements ThirstStorage {
	private final DeferredRegister<AttachmentType<?>>        attachments = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MOD_ID);
	private final Supplier<AttachmentType<ThirstState>>      thirst      = attachments.register("thirst", () -> AttachmentType.builder(ThirstState::hydrated).serialize(ThirstPersistence.CODEC).build());
	// Intentionally omit copyOnDeath: respawning starts fully hydrated, even with keepInventory.
	private final Supplier<AttachmentType<DehydrationTimer>> damageTimer = attachments.register("dehydration_timer", () -> AttachmentType.builder(DehydrationTimer::new).build());

	@Override
	public DehydrationTimer damageTimer (ServerPlayer player) {
		return player.getData(damageTimer);
	}

	public void register (IEventBus modBus) {
		attachments.register(modBus);
	}

	@Override
	public ThirstState get (ServerPlayer player) {
		return player.getData(thirst);
	}

	@Override
	public void set (ServerPlayer player, ThirstState state) {
		player.setData(thirst, state);
	}
}
