package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class NeoForgeTemperatureStorage implements TemperatureStorage {
	private final DeferredRegister<AttachmentType<?>>              attachments = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MOD_ID);
	private final Supplier<AttachmentType<TemperatureState>>       temperature = attachments.register("temperature", () -> AttachmentType.builder(TemperatureState::comfortable).serialize(TemperaturePersistence.CODEC).build());
	// No copyOnDeath: death respawn is comfortable even with keepInventory enabled.
	private final Supplier<AttachmentType<TemperatureUpdateClock>> clock       = attachments.register("temperature_clock", () -> AttachmentType.builder(TemperatureUpdateClock::new).build());
	private final Supplier<AttachmentType<TemperatureDamageTimer>> damageTimer = attachments.register("temperature_damage_timer", () -> AttachmentType.builder(TemperatureDamageTimer::new).build());

	@Override
	public TemperatureDamageTimer damageTimer (ServerPlayer player) {
		return player.getData(damageTimer);
	}

	public void register (IEventBus modBus) {
		attachments.register(modBus);
	}

	@Override
	public TemperatureState get (ServerPlayer player) {
		return player.getData(temperature);
	}

	@Override
	public void set (ServerPlayer player, TemperatureState state) {
		player.setData(temperature, state);
	}

	@Override
	public TemperatureUpdateClock clock (ServerPlayer player) {
		return player.getData(clock);
	}
}
