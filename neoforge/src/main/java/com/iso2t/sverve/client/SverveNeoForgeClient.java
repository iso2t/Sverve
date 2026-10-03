package com.iso2t.sverve.client;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.client.moisture.ClientMoistureState;
import com.iso2t.sverve.client.moisture.MoistureHud;
import com.iso2t.sverve.client.temperature.ClientTemperatureState;
import com.iso2t.sverve.client.temperature.TemperatureHud;
import com.iso2t.sverve.client.thirst.ClientThirstState;
import com.iso2t.sverve.client.thirst.ThirstHud;
import com.iso2t.sverve.network.moisture.MoistureSyncPayload;
import com.iso2t.sverve.network.temperature.TemperatureSyncPayload;
import com.iso2t.sverve.network.thirst.ThirstSyncPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public final class SverveNeoForgeClient {
	public SverveNeoForgeClient (IEventBus modBus) {
		var moisture = new ClientMoistureState();
		var moistureHud = new MoistureHud(moisture);
		modBus.addListener((RegisterClientPayloadHandlersEvent event) -> event.register(MoistureSyncPayload.TYPE, (payload, context) -> moisture.accept(payload.getSnapshot())));
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> moisture.clear());
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> moisture.clear());
		var state = new ClientThirstState();
		var hud = new ThirstHud(state);
		var temperature = new ClientTemperatureState();
		var temperatureHud = new TemperatureHud(temperature);
		modBus.addListener((RegisterClientPayloadHandlersEvent event) -> event.register(TemperatureSyncPayload.TYPE, (payload, context) -> temperature.accept(payload.getSnapshot())));
		modBus.addListener((RegisterGuiLayersEvent event) -> {
			event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, TemperatureHud.LAYER, (graphics, delta) -> temperatureHud.extract(graphics));
			event.registerAbove(TemperatureHud.LAYER, MoistureHud.LAYER, (graphics, delta) -> moistureHud.extract(graphics));
		});
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> temperature.clear());
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> temperature.clear());
		modBus.addListener((RegisterClientPayloadHandlersEvent event) -> event.register(ThirstSyncPayload.TYPE, (payload, context) -> state.accept(payload.getSnapshot())));
		modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAbove(VanillaGuiLayers.AIR_LEVEL, ThirstHud.LAYER, (graphics, delta) -> {
			if (!hud.isVisible()) return;
			var nativeHud = Minecraft.getInstance().gui.hud;
			hud.extract(graphics, graphics.guiHeight() - nativeHud.rightHeight);
			nativeHud.rightHeight += ThirstHud.ROW_HEIGHT;
		}));
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> state.clear());
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> state.clear());
	}
}
