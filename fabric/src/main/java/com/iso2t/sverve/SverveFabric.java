package com.iso2t.sverve;

import com.iso2t.sverve.network.moisture.FabricMoistureSyncTransport;
import com.iso2t.sverve.network.temperature.FabricTemperatureSyncTransport;
import com.iso2t.sverve.network.thirst.FabricThirstSyncTransport;
import com.iso2t.sverve.player.moisture.FabricMoistureStorage;
import com.iso2t.sverve.player.temperature.FabricTemperatureEvents;
import com.iso2t.sverve.player.temperature.FabricTemperatureStorage;
import com.iso2t.sverve.player.thirst.FabricThirstGameplay;
import com.iso2t.sverve.player.thirst.FabricThirstLifecycle;
import com.iso2t.sverve.player.thirst.FabricThirstStorage;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;

@Getter
public final class SverveFabric implements ModInitializer {
	private SverveRuntime runtime;

	@Override
	public void onInitialize () {
		runtime = Sverve.initialize(new FabricThirstStorage(), new FabricThirstSyncTransport(), new FabricTemperatureStorage(), new FabricTemperatureSyncTransport(), new FabricMoistureStorage(), new FabricMoistureSyncTransport());
		FabricThirstLifecycle.register(runtime.getThirstSynchronizer());
		FabricThirstGameplay.register(runtime.getThirstGameplay());
		FabricTemperatureEvents.register(runtime.getTemperatureGameplay());
	}
}
