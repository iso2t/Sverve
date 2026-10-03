package com.iso2t.sverve;

import com.iso2t.sverve.client.SverveFabricClient;
import com.iso2t.sverve.client.moisture.ClientMoistureState;
import com.iso2t.sverve.client.moisture.MoistureHud;
import com.iso2t.sverve.survival.moisture.MoistureState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Real owner packets, every fill, native visibility, dimension refresh, and disconnect cleanup.
 */
public final class MoistureClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest (ClientGameTestContext context) {
		var state = clientState();
		var hud = new MoistureHud(state);
		var config = runtime().getConfig().getMoisture();
		double drying = config.getDryingRate().get();
		double heatDrying = config.getHeatDryingRate().get();
		try (var world = context.worldBuilder().create()) {
			var connection = world.getConnection();
			world.getServer().runOnServer(server -> {
				config.getDryingRate().set(0.0);
				config.getHeatDryingRate().set(0.0);
				connection.getServerPlayer().setItemInHand(InteractionHand.OFF_HAND, Items.SHIELD.getDefaultInstance());
			});
			context.waitFor(client -> state.getSnapshot() != null);
			context.runOnClient(client -> require(!hud.isVisible(), "New dry players must have no wetness icon"));
			context.takeScreenshot("moisture-dry");
			for (int fill = 1; fill <= 7; fill++) {
				int expected = fill;
				double wetness = fill == 7 ? 1 : (fill - 0.5) / 7;
				world.getServer().runOnServer(server -> runtime().getPlayerMoisture().update(connection.getServerPlayer(), ignored -> new MoistureState(wetness)));
				context.waitFor(client -> state.getSnapshot().getFillUnits() == expected);
				context.runOnClient(client -> require(hud.isVisible(), "Wet Survival players must display their fill"));
				connection.waitForChunksRender();
				context.takeScreenshot("moisture-fill-" + fill);
			}
			world.getServer().runOnServer(server -> connection.getServerPlayer().setMainArm(HumanoidArm.LEFT));
			context.waitFor(client -> client.player.getMainArm() == HumanoidArm.LEFT);
			context.takeScreenshot("moisture-right-offhand");
			world.getServer().runOnServer(server -> connection.getServerPlayer().setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY));
			connection.waitForClientboundPackets();
			context.runOnClient(client -> {
				client.options.attackIndicator().set(AttackIndicatorStatus.HOTBAR);
				client.player.resetAttackStrengthTicker();
			});
			context.takeScreenshot("moisture-right-attack-indicator");
			context.runOnClient(client -> client.options.attackIndicator().set(AttackIndicatorStatus.CROSSHAIR));
			world.getServer().runOnServer(server -> connection.getServerPlayer().setMainArm(HumanoidArm.RIGHT));
			context.waitFor(client -> client.player.getMainArm() == HumanoidArm.RIGHT);
			world.getServer().runOnServer(server -> config.getEnabled().set(false));
			context.waitFor(client -> !state.getSnapshot().isEnabled());
			context.runOnClient(client -> require(!hud.isVisible(), "Disabled moisture must hide the icon"));
			context.takeScreenshot("moisture-disabled");
			world.getServer().runOnServer(server -> {
				config.getEnabled().set(true);
				runtime().getConfig().getTemperature().getEnabled().set(false);
			});
			context.waitFor(client -> state.getSnapshot().isEnabled());
			context.runOnClient(client -> require(hud.isVisible(), "Wetness must remain visible with temperature disabled"));
			for (var mode : new GameType[] { GameType.CREATIVE, GameType.SPECTATOR }) {
				world.getServer().runOnServer(server -> connection.getServerPlayer().setGameMode(mode));
				connection.waitForClientboundPackets();
				context.runOnClient(client -> require(!hud.isVisible(), mode + " must hide the wetness icon"));
			}
			world.getServer().runOnServer(server -> connection.getServerPlayer().setGameMode(GameType.SURVIVAL));
			connection.waitForClientboundPackets();
			context.runOnClient(client -> {
				require(hud.isVisible(), "Survival must restore the wetness icon");
				client.gui.hud.toggle();
				require(!hud.isVisible(), "F1 must hide the wetness icon");
			});
			context.takeScreenshot("moisture-hidden-hud");
			context.runOnClient(client -> {
				client.gui.hud.toggle();
				var player = client.player;
				client.setCameraEntity(null);
				require(!hud.isVisible(), "A different camera must hide the wetness icon");
				client.setCameraEntity(player);
				float health = player.getHealth();
				player.setHealth(0);
				require(!hud.isVisible(), "Dead players must hide the wetness icon");
				player.setHealth(health);
			});
			world.getServer().runOnServer(server -> connection.getServerPlayer().teleportTo(server.getLevel(Level.NETHER), 0.5, 80, 0.5, Set.of(), 0, 0, true));
			context.waitFor(client -> client.level.dimension() == Level.NETHER && state.getSnapshot().getFillUnits() == 7);
			connection.waitForChunksRender();
			context.runOnClient(client -> require(hud.isVisible(), "Dimension refresh must preserve the wetness display"));
			context.takeScreenshot("moisture-nether");
			world.getServer().runOnServer(server -> runtime().getPlayerMoisture().update(connection.getServerPlayer(), ignored -> MoistureState.dry()));
			context.waitFor(client -> state.getSnapshot().getFillUnits() == 0);
			context.runOnClient(client -> require(!hud.isVisible(), "Drying completely must hide the icon"));
		} finally {
			config.getDryingRate().set(drying);
			config.getHeatDryingRate().set(heatDrying);
			config.getEnabled().set(true);
			runtime().getConfig().getTemperature().getEnabled().set(true);
		}
		context.runOnClient(client -> require(state.getSnapshot() == null, "Disconnect must clear cached wetness"));
	}

	private static SverveRuntime runtime () {
		return FabricLoader.getInstance().getEntrypoints("main", ModInitializer.class).stream().filter(SverveFabric.class::isInstance).map(SverveFabric.class::cast).findFirst().orElseThrow().getRuntime();
	}

	private static ClientMoistureState clientState () {
		return FabricLoader.getInstance().getEntrypoints("client", ClientModInitializer.class).stream().filter(SverveFabricClient.class::isInstance).map(SverveFabricClient.class::cast).findFirst().orElseThrow().getMoisture();
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
