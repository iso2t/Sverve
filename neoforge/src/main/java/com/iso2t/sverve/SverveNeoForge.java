package com.iso2t.sverve;

import com.iso2t.sverve.network.moisture.NeoForgeMoistureSyncTransport;
import com.iso2t.sverve.network.temperature.NeoForgeTemperatureSyncTransport;
import com.iso2t.sverve.network.thirst.NeoForgeThirstSyncTransport;
import com.iso2t.sverve.player.moisture.NeoForgeMoistureStorage;
import com.iso2t.sverve.player.temperature.NeoForgeTemperatureEvents;
import com.iso2t.sverve.player.temperature.NeoForgeTemperatureStorage;
import com.iso2t.sverve.player.thirst.NeoForgeThirstGameplay;
import com.iso2t.sverve.player.thirst.NeoForgeThirstLifecycle;
import com.iso2t.sverve.player.thirst.NeoForgeThirstStorage;
import lombok.Getter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Getter
@Mod(Constants.MOD_ID)
public final class SverveNeoForge {
	private final SverveRuntime runtime;

	public SverveNeoForge (IEventBus modBus) {
		var storage = new NeoForgeThirstStorage();
		storage.register(modBus);
		var transport = new NeoForgeThirstSyncTransport();
		transport.register(modBus);
		var temperatureStorage = new NeoForgeTemperatureStorage();
		temperatureStorage.register(modBus);
		var temperatureTransport = new NeoForgeTemperatureSyncTransport();
		temperatureTransport.register(modBus);
		var moistureStorage = new NeoForgeMoistureStorage();
		moistureStorage.register(modBus);
		var moistureTransport = new NeoForgeMoistureSyncTransport();
		moistureTransport.register(modBus);
		runtime = Sverve.initialize(storage, transport, temperatureStorage, temperatureTransport, moistureStorage, moistureTransport);
		NeoForgeThirstLifecycle.register(runtime.getThirstSynchronizer());
		NeoForgeThirstGameplay.register(runtime.getThirstGameplay());
		NeoForgeTemperatureEvents.register(runtime.getTemperatureGameplay());
	}
}
