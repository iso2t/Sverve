package com.iso2t.sverve.compat.heavyinventories;

import com.iso2t.heavyinventories.api.PlayerWeightSnapshot;
import com.iso2t.heavyinventories.api.plugin.HIPlugin;
import com.iso2t.heavyinventories.api.plugin.HeavyInventoriesPlugin;
import com.iso2t.heavyinventories.api.plugin.PluginRegistration;
import com.iso2t.sverve.Constants;
import com.iso2t.sverve.player.exertion.CarriedLoad;
import net.minecraft.resources.Identifier;

/**
 * Loaded exclusively by HI's plugin discovery; Sverve's normal bootstrap has no HI references.
 */
@HIPlugin
public final class SverveWeightPlugin implements HeavyInventoriesPlugin {
	@Override
	public Identifier id () {
		return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "carried_load");
	}

	@Override
	public void register (PluginRegistration registration) {
		var weights = registration.weights();
		CarriedLoad.install(player -> weights.player(player).filter(PlayerWeightSnapshot::effectsApply).map(snapshot -> snapshot.loadRatio().orElse(0.0)).orElse(0.0));
		Constants.LOG.info("Heavy Inventories integration registered");
	}
}
