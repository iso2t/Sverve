package com.iso2t.sverve;

import com.iso2t.sverve.client.SverveFabricClient;
import com.iso2t.sverve.client.thirst.ClientThirstState;
import com.iso2t.sverve.client.thirst.ThirstHud;
import com.iso2t.sverve.survival.thirst.ThirstState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * End-to-end network/HUD checks with screenshots from a real Minecraft client.
 */
public final class ThirstClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest (ClientGameTestContext context) {
		var clientState = clientState();
		var hud = new ThirstHud(clientState);
		try (var world = context.worldBuilder().create()) {
			var connection = world.getConnection();
			world.getServer().runOnServer(server -> {
				var player = connection.getServerPlayer();
				player.setGameMode(GameType.SURVIVAL);
				runtime().getPlayerThirst().update(player, state -> new ThirstState(0.425));
			});
			context.waitFor(client -> clientState.getSnapshot() != null && clientState.getSnapshot().getHalfUnits() == 9);
			context.runOnClient(client -> {
				require(hud.isVisible(), "A synchronized Survival player must see the thirst bar");
				require(HudStatusBarHeightRegistry.getHeight(ThirstHud.LAYER) == 49, "Thirst must be one row above vanilla's hunger bar, which starts 39 pixels from the bottom");
			});
			connection.waitForChunksRender();
			context.takeScreenshot("thirst-partial");

			world.getServer().runOnServer(server -> {
				var player = connection.getServerPlayer();
				player.setItemInHand(InteractionHand.MAIN_HAND, Items.POTION.getDefaultInstance());
				player.startUsingItem(InteractionHand.MAIN_HAND);
			});
			context.waitTicks(35);
			connection.waitForClientboundPackets();
			context.runOnClient(client -> {
				require(clientState.getSnapshot().getHalfUnits() == 15, "Completed drinking must update the client from 9 to 15 half icons");
				require(client.player.getMainHandItem().is(Items.GLASS_BOTTLE), "Drinking must keep vanilla's glass bottle remainder");
			});
			context.takeScreenshot("thirst-after-drinking");

			world.getServer().runOnServer(server -> connection.getServerPlayer().setAirSupply(150));
			context.waitTicks(3);
			connection.waitForClientboundPackets();
			context.takeScreenshot("thirst-with-air");

			world.getServer().runOnServer(server -> runtime().getConfig().getThirst().getEnabled().set(false));
			context.waitFor(client -> !clientState.getSnapshot().isEnabled());
			context.runOnClient(client -> require(!hud.isVisible(), "The server enable switch must hide the bar"));
			context.takeScreenshot("thirst-disabled");
			world.getServer().runOnServer(server -> runtime().getConfig().getThirst().getEnabled().set(true));
			context.waitFor(client -> clientState.getSnapshot().isEnabled());

			world.getServer().runOnServer(server -> connection.getServerPlayer().setGameMode(GameType.CREATIVE));
			connection.waitForClientboundPackets();
			context.runOnClient(client -> require(!hud.isVisible(), "Creative must hide thirst without deleting the received state"));
			context.takeScreenshot("thirst-creative");
			world.getServer().runOnServer(server -> connection.getServerPlayer().setGameMode(GameType.SURVIVAL));
			connection.waitForClientboundPackets();
			context.runOnClient(client -> {
				require(hud.isVisible(), "Returning to Survival must show the saved thirst");
				client.gui.hud.toggle();
				require(!hud.isVisible(), "F1 must hide the thirst bar with the rest of the HUD");
			});
			context.takeScreenshot("thirst-hidden-hud");
			context.runOnClient(client -> client.gui.hud.toggle());
		}
		context.runOnClient(client -> require(clientState.getSnapshot() == null, "Leaving the world must clear the client snapshot"));
	}

	private static SverveRuntime runtime () {
		return FabricLoader.getInstance().getEntrypoints("main", ModInitializer.class).stream().filter(SverveFabric.class::isInstance).map(SverveFabric.class::cast).findFirst().orElseThrow().getRuntime();
	}

	private static ClientThirstState clientState () {
		return FabricLoader.getInstance().getEntrypoints("client", ClientModInitializer.class).stream().filter(SverveFabricClient.class::isInstance).map(SverveFabricClient.class::cast).findFirst().orElseThrow().getThirst();
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
