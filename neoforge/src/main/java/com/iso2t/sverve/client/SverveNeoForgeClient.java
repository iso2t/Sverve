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

	private final ClientMoistureState    moisture    = new ClientMoistureState();
	private final ClientTemperatureState temperature = new ClientTemperatureState();
	private final ClientThirstState      thirst      = new ClientThirstState();

	public SverveNeoForgeClient (IEventBus modBus) {
		registerNetworking(modBus);
		registerConnections();
		registerHud(modBus);
	}

	private void registerNetworking (IEventBus modBus) {
		modBus.addListener((RegisterClientPayloadHandlersEvent event) -> {
			event.register(MoistureSyncPayload.TYPE, (payload, _) -> moisture.accept(payload.snapshot()));
			event.register(TemperatureSyncPayload.TYPE, (payload, _) -> temperature.accept(payload.snapshot()));
			event.register(ThirstSyncPayload.TYPE, (payload, _) -> thirst.accept(payload.snapshot()));
		});
	}

	private void registerConnections () {
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn _) -> clear());
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut _) -> clear());
	}

	private void clear () {
		moisture.clear();
		temperature.clear();
		thirst.clear();
	}

	private void registerHud (IEventBus modBus) {
		var moistureHud = new MoistureHud(moisture);
		var temperatureHud = new TemperatureHud(temperature);
		var thirstHud = new ThirstHud(thirst);
		modBus.addListener((RegisterGuiLayersEvent event) -> {
			event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, TemperatureHud.LAYER, (graphics, _) -> temperatureHud.extract(graphics));
			event.registerAbove(TemperatureHud.LAYER, MoistureHud.LAYER, (graphics, _) -> moistureHud.extract(graphics));
			event.registerAbove(VanillaGuiLayers.AIR_LEVEL, ThirstHud.LAYER, (graphics, _) -> {
				if (!thirstHud.isVisible()) return;
				var nativeHud = Minecraft.getInstance().gui.hud;
				thirstHud.extract(graphics, graphics.guiHeight() - nativeHud.rightHeight);
				nativeHud.rightHeight += ThirstHud.ROW_HEIGHT;
			});
		});
	}
}
