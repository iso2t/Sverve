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
		ClientPlayNetworking.registerGlobalReceiver(MoistureSyncPayload.TYPE, (payload, context) -> moisture.accept(payload.getSnapshot()));
		ClientPlayConnectionEvents.INIT.register((handler, client) -> moisture.clear());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> moisture.clear());
		var moistureHud = new MoistureHud(moisture);
		ClientPlayNetworking.registerGlobalReceiver(ThirstSyncPayload.TYPE, (payload, context) -> thirst.accept(payload.getSnapshot()));
		ClientPlayConnectionEvents.INIT.register((handler, client) -> thirst.clear());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> thirst.clear());
		ClientPlayNetworking.registerGlobalReceiver(TemperatureSyncPayload.TYPE, (payload, context) -> temperature.accept(payload.getSnapshot()));
		ClientPlayConnectionEvents.INIT.register((handler, client) -> temperature.clear());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> temperature.clear());
		var temperatureHud = new TemperatureHud(temperature);
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, TemperatureHud.LAYER, (graphics, delta) -> temperatureHud.extract(graphics));
		HudElementRegistry.attachElementAfter(TemperatureHud.LAYER, MoistureHud.LAYER, (graphics, delta) -> moistureHud.extract(graphics));
		var hud = new ThirstHud(thirst);
		HudElementRegistry.attachElementAfter(VanillaHudElements.AIR_BAR, ThirstHud.LAYER, (graphics, delta) -> hud.extract(graphics, graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(ThirstHud.LAYER)));
		HudStatusBarHeightRegistry.addRight(ThirstHud.LAYER, player -> hud.isVisible() ? ThirstHud.ROW_HEIGHT : 0);
	}
}
