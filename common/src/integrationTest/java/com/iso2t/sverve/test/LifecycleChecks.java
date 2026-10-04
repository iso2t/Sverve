package com.iso2t.sverve.test;

import com.iso2t.sverve.player.moisture.MoistureUpdateClock;
import com.iso2t.sverve.player.temperature.TemperatureDamageTimer;
import com.iso2t.sverve.player.temperature.TemperatureUpdateClock;
import com.iso2t.sverve.player.thirst.DehydrationTimer;
import com.iso2t.sverve.survival.moisture.MoistureState;
import com.iso2t.sverve.survival.temperature.TemperatureBand;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import com.iso2t.sverve.survival.thirst.ThirstState;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.experimental.UtilityClass;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * Runs only in isolated dedicated-server test launches; seed and verify must be different JVMs.
 */
@UtilityClass
public class LifecycleChecks {
	private static final GameProfile FIRST        = new GameProfile(UUID.fromString("ac93841c-5797-4d06-b1c0-c60f03030301"), "survival-first");
	private static final GameProfile SECOND       = new GameProfile(UUID.fromString("ac93841c-5797-4d06-b1c0-c60f03030302"), "survival-second");
	private static final State       FIRST_STATE  = new State(0.375, -0.625, 0.75);
	private static final State       SECOND_STATE = new State(0.875, 0.625, 0.125);

	public interface Attachments {
		Object get (ServerPlayer player, String name);

		void set (ServerPlayer player, String name, Object value);
	}

	private record State(double thirst, double temperature, double moisture) {
		void write (ServerPlayer player, Attachments access) {
			access.set(player, "thirst", new ThirstState(thirst));
			access.set(player, "temperature", new TemperatureState(temperature));
			access.set(player, "moisture", new MoistureState(moisture));
		}

		void check (ServerPlayer player, Attachments access) {
			near(((ThirstState) access.get(player, "thirst")).getHydration(), thirst, "thirst");
			near(((TemperatureState) access.get(player, "temperature")).getExposure(), temperature, "temperature");
			near(((MoistureState) access.get(player, "moisture")).getWetness(), moisture, "moisture");
		}
	}

	public static void run (MinecraftServer server, Attachments access) {
		String phase = System.getProperty("sverve.check.phase");
		Path report = Path.of(phase + "-result.txt");
		try {
			if ("seed".equals(phase)) {
				String token = UUID.randomUUID().toString();
				Files.writeString(Path.of("seed-token.txt"), token);
				for (var profile : new GameProfile[] { FIRST, SECOND }) {
					var player = join(server, profile, false);
					(profile == FIRST ? FIRST_STATE : SECOND_STATE).write(player, access);
					player.getInventory().clearContent();
					player.getInventory().setSelectedSlot(0);
					player.getInventory().setItem(0, Items.POTION.getDefaultInstance().copyWithCount(16));
					var marker = Items.PAPER.getDefaultInstance();
					marker.set(DataComponents.CUSTOM_NAME, Component.literal(token));
					player.getInventory().setItem(1, marker);
					advanceTransient(player, access);
				}
				Files.writeString(Path.of("seed-pid.txt"), Long.toString(ProcessHandle.current().pid()));
				// Keep both players connected. Normal server shutdown must save them.
				Files.writeString(report, "PASS normal shutdown with two connected players\n");
			} else if ("verify".equals(phase)) {
				require(!Files.readString(Path.of("seed-pid.txt")).equals(Long.toString(ProcessHandle.current().pid())), "Restart must launch a new JVM");
				var first = join(server, FIRST, true);
				var second = join(server, SECOND, true);
				String token = Files.readString(Path.of("seed-token.txt"));
				for (var player : new ServerPlayer[] { first, second }) {
					var marker = player.getInventory().getItem(1);
					require(marker.is(Items.PAPER) && Component.literal(token).equals(marker.get(DataComponents.CUSTOM_NAME)), "Shutdown must save this run's unique marker, not reuse an old player file");
				}
				FIRST_STATE.check(first, access);
				SECOND_STATE.check(second, access);
				freshTransient(first, access);
				freshTransient(second, access);
				require(first.getMainHandItem().is(Items.POTION) && first.getMainHandItem().getCount() == 16, "Water inventory must persist across restart");

				first.startUsingItem(InteractionHand.MAIN_HAND);
				for (int tick = 0; tick < 32; tick++) first.doTick();
				// NeoForge posts survival ticks from doTick; Fabric posts them at server tick end.
				double hydration = ((ThirstState) access.get(first, "thirst")).getHydration();
				require(hydration > 0.67 && hydration <= 0.675 + 1e-10, "Completed drinking must restore water while allowing normal drain: hydration=" + hydration + ", health=" + first.getHealth() + ", hand=" + first.getMainHandItem());
				SECOND_STATE.check(second, access);
				require(first.getMainHandItem().getCount() == 15 && second.getMainHandItem().getCount() == 16, "Drinking must change only its owner");
				FIRST_STATE.write(first, access);
				advanceTransient(first, access);
				server.getPlayerList().remove(first);
				first = join(server, FIRST, true);
				FIRST_STATE.check(first, access);
				SECOND_STATE.check(second, access);
				freshTransient(first, access);

				first.teleportTo(server.getLevel(Level.NETHER), 0.5, 160, 0.5, Set.of(), 0, 0, true);
				FIRST_STATE.check(first, access);
				first.teleportTo(server.getLevel(Level.END), 0.5, 160, 0.5, Set.of(), 0, 0, true);
				FIRST_STATE.check(first, access);
				first.showEndCredits();
				var connection = first.connection;
				connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
				first = connection.player;
				first.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
				require(first.level().dimension() == Level.OVERWORLD, "End credits must return the player to the Overworld");
				FIRST_STATE.check(first, access);
				SECOND_STATE.check(second, access);
				freshTransient(first, access);
				HeatSamplingChecks.run(first, Path.of("heat-sampling.csv"));
				Files.writeString(report, "PASS new-JVM persistence; reconnect; independent player states and drinking; fresh clocks and damage timers; Nether transfer; End credits return; heat sampling\n");
			} else throw new IllegalArgumentException("Unknown lifecycle phase " + phase);
		} catch (Throwable failure) {
			var detail = new StringWriter();
			failure.printStackTrace(new PrintWriter(detail));
			try {
				Files.writeString(report, "FAIL\n" + detail);
			} catch (Exception writeFailure) {
				failure.addSuppressed(writeFailure);
			}
			failure.printStackTrace();
		} finally {
			server.halt(false);
		}
	}

	private static ServerPlayer join (MinecraftServer server, GameProfile profile, boolean mustExist) {
		var player = new ServerPlayer(server, server.overworld(), profile, ClientInformation.createDefault());
		var saved = server.getPlayerList().loadPlayerData(player.nameAndId());
		require(!mustExist || saved.isPresent(), "Saved player must exist: " + profile.name());
		saved.ifPresent(tag -> player.load(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), tag)));
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(profile, false));
		player.setGameMode(GameType.SURVIVAL);
		// The benchmark intentionally uses lava; repeated runs must start living, safe fixtures.
		player.setHealth(player.getMaxHealth());
		player.clearFire();
		player.setPermanentlyInvulnerable(true);
		player.setNoGravity(true);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		player.setPos(8.5, 160, 8.5);
		return player;
	}

	private static void advanceTransient (ServerPlayer player, Attachments access) {
		var temperature = (TemperatureUpdateClock) access.get(player, "temperature_clock");
		var moisture = (MoistureUpdateClock) access.get(player, "moisture_clock");
		temperature.reset();
		moisture.reset();
		for (int i = 0; i < 19; i++) {
			temperature.advance();
			moisture.advance();
		}
		((DehydrationTimer) access.get(player, "dehydration_timer")).advance(100);
		((TemperatureDamageTimer) access.get(player, "temperature_damage_timer")).advance(TemperatureBand.HOT, 100);
	}

	private static void freshTransient (ServerPlayer player, Attachments access) {
		require(((DehydrationTimer) access.get(player, "dehydration_timer")).getElapsedTicks() == 0, "Dehydration timer must reset");
		require(((TemperatureDamageTimer) access.get(player, "temperature_damage_timer")).getElapsedTicks() == 0, "Temperature damage timer must reset");
		var temperature = (TemperatureUpdateClock) access.get(player, "temperature_clock");
		var moisture = (MoistureUpdateClock) access.get(player, "moisture_clock");
		for (int i = 0; i < 19; i++) {
			require(!temperature.advance() && !moisture.advance(), "Reconnect must start a full sampling interval");
		}
		require(temperature.advance() && moisture.advance(), "Sampling must resume on the twentieth tick");
	}

	private static void near (double actual, double expected, String label) {
		require(Math.abs(actual - expected) < 1e-10, label + ": expected " + expected + ", got " + actual);
	}

	static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
