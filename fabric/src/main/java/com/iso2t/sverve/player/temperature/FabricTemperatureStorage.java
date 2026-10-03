package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class FabricTemperatureStorage implements TemperatureStorage {
	private final AttachmentType<TemperatureState>       temperature = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "temperature"), builder -> builder.initializer(TemperatureState::comfortable).persistent(TemperaturePersistence.CODEC.codec()));
	// No copyOnDeath: death respawn is comfortable even with keepInventory enabled.
	private final AttachmentType<TemperatureUpdateClock> clock       = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "temperature_clock"), builder -> builder.initializer(TemperatureUpdateClock::new));

	private final AttachmentType<TemperatureDamageTimer> damageTimer = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "temperature_damage_timer"), builder -> builder.initializer(TemperatureDamageTimer::new));

	@Override
	public TemperatureDamageTimer damageTimer (ServerPlayer player) {
		return player.getAttachedOrCreate(damageTimer);
	}

	@Override
	public TemperatureState get (ServerPlayer player) {
		return player.getAttachedOrCreate(temperature);
	}

	@Override
	public void set (ServerPlayer player, TemperatureState state) {
		player.setAttached(temperature, state);
	}

	@Override
	public TemperatureUpdateClock clock (ServerPlayer player) {
		return player.getAttachedOrCreate(clock);
	}
}
