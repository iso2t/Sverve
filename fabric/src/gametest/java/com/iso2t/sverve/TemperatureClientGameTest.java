package com.iso2t.sverve;

import com.iso2t.sverve.client.SverveFabricClient;
import com.iso2t.sverve.client.temperature.ClientTemperatureState;
import com.iso2t.sverve.client.temperature.TemperatureHud;
import com.iso2t.sverve.survival.temperature.TemperatureBand;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

/**
 * Exercises owner packets, every status head, and center placement through a real client HUD.
 */
public final class TemperatureClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest (ClientGameTestContext context) {
		var state = clientState();
		var hud = new TemperatureHud(state);
		try (var world = context.worldBuilder().create()) {
			var connection = world.getConnection();
			double[] exposures = { -1, -0.5, 0, 0.5, 1 };
			for (var band : TemperatureBand.values()) {
				world.getServer().runOnServer(server -> {
					var player = connection.getServerPlayer();
					player.setGameMode(GameType.SURVIVAL);
					player.setExperienceLevels(25);
					runtime().getPlayerTemperature().update(player, ignored -> new TemperatureState(exposures[band.getId()]));
				});
				context.waitFor(client -> state.getSnapshot() != null && state.getSnapshot().getBand() == band && client.player.experienceLevel == 25);
				context.runOnClient(client -> {
					require(hud.isVisible(), "Survival must display " + band);
				});
				connection.waitForChunksRender();
				context.takeScreenshot("temperature-" + band.name().toLowerCase(java.util.Locale.ROOT));
			}

			double responseRate = runtime().getConfig().getTemperature().getResponseRate().get();
			try {
				world.getServer().runOnServer(server -> {
					runtime().getConfig().getTemperature().getResponseRate().set(0.0);
					connection.getServerPlayer().setPermanentlyInvulnerable(true);
					connection.getServerPlayer().setNoGravity(true);
				});
				for (var dimension : List.of(Level.NETHER, Level.END, Level.OVERWORLD)) {
					world.getServer().runOnServer(server -> {
						var player = connection.getServerPlayer();
						runtime().getPlayerTemperature().update(player, ignored -> TemperatureState.comfortable());
						player.teleportTo(server.getLevel(dimension), 0.5, 80, 0.5, Set.of(), 0, 0, true);
					});
					context.waitFor(client -> client.level.dimension() == dimension
							&& state.getSnapshot().getBand() == TemperatureBand.NORMAL && hud.isVisible());
					connection.waitForChunksRender();
					context.takeScreenshot("temperature-" + dimension.identifier().getPath() + "-player-face");
					world.getServer().runOnServer(server -> runtime().getPlayerTemperature().update(
							connection.getServerPlayer(), ignored -> new TemperatureState(1)));
					context.waitFor(client -> state.getSnapshot().getBand() == TemperatureBand.HOT && hud.isVisible());
					context.takeScreenshot("temperature-" + dimension.identifier().getPath() + "-hot");
				}
			} finally {
				runtime().getConfig().getTemperature().getResponseRate().set(responseRate);
			}

			world.getServer().runOnServer(server -> {
				var player = connection.getServerPlayer();
				runtime().getPlayerTemperature().update(player, ignored -> TemperatureState.comfortable());
				player.setItemInHand(InteractionHand.OFF_HAND, Items.SHIELD.getDefaultInstance());
			});
			context.waitFor(client -> state.getSnapshot().getBand() == TemperatureBand.NORMAL);
			connection.waitForClientboundPackets();
			context.takeScreenshot("temperature-with-offhand");
			world.getServer().runOnServer(server -> runtime().getPlayerTemperature().update(connection.getServerPlayer(), ignored -> new TemperatureState(0.26)));
			context.waitTicks(2);
			connection.waitForClientboundPackets();
			context.runOnClient(client -> require(state.getSnapshot().getBand() == TemperatureBand.NORMAL, "Small boundary fluctuations must keep the previous icon"));
			world.getServer().runOnServer(server -> runtime().getPlayerTemperature().update(connection.getServerPlayer(), ignored -> new TemperatureState(0.30)));
			context.waitFor(client -> state.getSnapshot().getBand() == TemperatureBand.WARM);

			world.getServer().runOnServer(server -> runtime().getConfig().getTemperature().getEnabled().set(false));
			context.waitFor(client -> !state.getSnapshot().isEnabled());
			context.runOnClient(client -> {
				require(!hud.isVisible(), "Server config must hide the icon");
			});
			context.takeScreenshot("temperature-disabled");
			world.getServer().runOnServer(server -> runtime().getConfig().getTemperature().getEnabled().set(true));
			context.waitFor(client -> state.getSnapshot().isEnabled());
			for (var mode : new GameType[] { GameType.CREATIVE, GameType.SPECTATOR }) {
				world.getServer().runOnServer(server -> connection.getServerPlayer().setGameMode(mode));
				connection.waitForClientboundPackets();
				context.runOnClient(client -> require(!hud.isVisible(), mode + " must hide the icon"));
			}
			world.getServer().runOnServer(server -> connection.getServerPlayer().setGameMode(GameType.SURVIVAL));
			connection.waitForClientboundPackets();
			context.runOnClient(client -> {
				require(hud.isVisible(), "Returning to Survival must restore the icon");
				client.gui.hud.toggle();
				require(!hud.isVisible(), "F1 must hide the icon");
			});
			context.takeScreenshot("temperature-hidden-hud");
			context.runOnClient(client -> client.gui.hud.toggle());
		}
		context.runOnClient(client -> require(state.getSnapshot() == null, "Disconnect must clear temperature"));
	}

	private static SverveRuntime runtime () {
		return FabricLoader.getInstance().getEntrypoints("main", ModInitializer.class).stream().filter(SverveFabric.class::isInstance).map(SverveFabric.class::cast).findFirst().orElseThrow().getRuntime();
	}

	private static ClientTemperatureState clientState () {
		return FabricLoader.getInstance().getEntrypoints("client", ClientModInitializer.class).stream().filter(SverveFabricClient.class::isInstance).map(SverveFabricClient.class::cast).findFirst().orElseThrow().getTemperature();
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
