package com.iso2t.sverve.client;

import com.iso2t.sverve.client.moisture.ClientMoistureState;
import com.iso2t.sverve.client.moisture.MoistureHud;
import com.iso2t.sverve.client.temperature.ClientTemperatureState;
import com.iso2t.sverve.client.temperature.TemperatureHud;
import com.iso2t.sverve.client.thirst.ClientThirstState;
import com.iso2t.sverve.client.thirst.ThirstHud;
import com.iso2t.sverve.network.moisture.MoistureSyncPayload;
import com.iso2t.sverve.network.temperature.TemperatureSyncPayload;
import com.iso2t.sverve.network.thirst.ThirstSyncPayload;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public final class SverveFabricClient implements ClientModInitializer {

	@Getter
	private final ClientThirstState      thirst      = new ClientThirstState();
	@Getter
	private final ClientTemperatureState temperature = new ClientTemperatureState();
	@Getter
	private final ClientMoistureState    moisture    = new ClientMoistureState();

	@Override
	public void onInitializeClient () {
		registerNetworking();
		registerConnections();
		registerHud();
	}

	private void registerNetworking () {
		ClientPlayNetworking.registerGlobalReceiver(MoistureSyncPayload.TYPE, (payload, _) -> moisture.accept(payload.snapshot()));
		ClientPlayNetworking.registerGlobalReceiver(ThirstSyncPayload.TYPE, (payload, _) -> thirst.accept(payload.snapshot()));
		ClientPlayNetworking.registerGlobalReceiver(TemperatureSyncPayload.TYPE, (payload, _) -> temperature.accept(payload.snapshot()));
	}

	private void registerConnections () {
		ClientPlayConnectionEvents.INIT.register((_, _) -> clear());
		ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> clear());
	}

	private void clear () {
		moisture.clear();
		thirst.clear();
		temperature.clear();
	}

	private void registerHud () {
		var moistureHud = new MoistureHud(moisture);
		var temperatureHud = new TemperatureHud(temperature);
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, TemperatureHud.LAYER, (graphics, _) -> temperatureHud.extract(graphics));
		HudElementRegistry.attachElementAfter(TemperatureHud.LAYER, MoistureHud.LAYER, (graphics, _) -> moistureHud.extract(graphics));
		var hud = new ThirstHud(thirst);
		HudElementRegistry.attachElementAfter(VanillaHudElements.AIR_BAR, ThirstHud.LAYER, (graphics, _) -> hud.extract(graphics, graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(ThirstHud.LAYER)));
		HudStatusBarHeightRegistry.addRight(ThirstHud.LAYER, _ -> hud.isVisible() ? ThirstHud.ROW_HEIGHT : 0);
	}
}
