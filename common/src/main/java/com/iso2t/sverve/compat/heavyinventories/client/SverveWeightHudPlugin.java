package com.iso2t.sverve.compat.heavyinventories.client;

import com.iso2t.heavyinventories.api.client.ClientPluginRegistration;
import com.iso2t.heavyinventories.api.client.HeavyInventoriesClientPlugin;
import com.iso2t.heavyinventories.api.client.HudElement;
import com.iso2t.heavyinventories.api.plugin.HIPlugin;
import com.iso2t.sverve.Constants;
import net.minecraft.resources.Identifier;

@HIPlugin(HIPlugin.Side.CLIENT)
public final class SverveWeightHudPlugin implements HeavyInventoriesClientPlugin {

	public static final Identifier OWNER = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "load_bar");

	@Override
	public Identifier id () {
		return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "weight_hud");
	}

	@Override
	public void register (ClientPluginRegistration registration) {
		registration.hud().owner(OWNER, HudElement.RING, 100, new LoadBarHud());
		Constants.LOG.info("Heavy Inventories compact load bar registered");
	}
}
