package com.iso2t.sverve;

import com.iso2t.sverve.network.thirst.ThirstSyncPayload;
import com.iso2t.sverve.network.thirst.ThirstSyncTracker;
import com.iso2t.sverve.network.thirst.ThirstSyncTransport;
import com.iso2t.sverve.network.thirst.ThirstSynchronizer;
import com.iso2t.sverve.player.thirst.PlayerThirst;
import com.iso2t.sverve.player.thirst.ThirstGameplay;
import com.iso2t.sverve.survival.thirst.ThirstConfig;
import com.iso2t.sverve.survival.thirst.ThirstState;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test-only mod: exercises real player attachments, item use, and the Fabric completion mixin.
 */
public class ThirstGameTests {
	@GameTest
	public void completedWaterBottleRestoresHydrationAndReturnsAnEmptyBottle (GameTestHelper helper) {
		ServerPlayer player = join(helper, GameType.SURVIVAL);
		try {
			seed(player, 0.2);
			drink(player, Items.POTION.getDefaultInstance(), 32);
			near(helper, thirst().get(player).getHydration(), 0.5);
			helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "Vanilla must return the glass bottle");
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void canceledDrinkingDoesNotRestoreHydrationOrConsumeTheBottle (GameTestHelper helper) {
		ServerPlayer player = join(helper, GameType.SURVIVAL);
		try {
			seed(player, 0.2);
			drink(player, Items.POTION.getDefaultInstance(), 10);
			player.releaseUsingItem();
			for (int i = 0; i < 40; i++) player.doTick();
			near(helper, thirst().get(player).getHydration(), 0.2);
			helper.assertTrue(ThirstGameplay.isPlainWaterBottle(player.getMainHandItem()), "Canceled use must leave the water bottle");
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void drinkingCapsHydrationAndDoesNotChangeAnotherPlayer (GameTestHelper helper) {
		ServerPlayer first = join(helper, GameType.SURVIVAL);
		ServerPlayer second = join(helper, GameType.SURVIVAL);
		try {
			seed(first, 0.9);
			seed(second, 0.25);
			drink(first, Items.POTION.getDefaultInstance(), 32);
			near(helper, thirst().get(first).getHydration(), 1.0);
			near(helper, thirst().get(second).getHydration(), 0.25);
		} finally {
			leave(first);
			leave(second);
		}
		helper.succeed();
	}

	@GameTest
	public void brewedPotionsAndThrownWaterDoNotRestoreHydration (GameTestHelper helper) {
		ServerPlayer player = join(helper, GameType.SURVIVAL);
		try {
			seed(player, 0.2);
			ItemStack potion = Items.POTION.getDefaultInstance();
			potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
			drink(player, potion, 32);
			near(helper, thirst().get(player).getHydration(), 0.2);
			helper.assertTrue(!ThirstGameplay.isPlainWaterBottle(Items.SPLASH_POTION.getDefaultInstance()), "Splash water must not hydrate");
			helper.assertTrue(!ThirstGameplay.isPlainWaterBottle(Items.LINGERING_POTION.getDefaultInstance()), "Lingering water must not hydrate");
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void serverTickDrainsSprintingPlayersFasterAndPreservesIndependentState (GameTestHelper helper) {
		ServerPlayer idle = join(helper, GameType.SURVIVAL);
		ServerPlayer sprinting = join(helper, GameType.SURVIVAL);
		try {
			seed(idle, 0.8);
			seed(sprinting, 0.4);
			sprinting.setSprinting(true);
			for (int i = 0; i < 20; i++) ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(helper.getLevel().getServer());
			near(helper, thirst().get(idle).getHydration(), 0.8 - 1.0 / 1200.0);
			near(helper, thirst().get(sprinting).getHydration(), 0.4 - 2.0 / 1200.0);
		} finally {
			leave(idle);
			leave(sprinting);
		}
		helper.succeed();
	}

	@GameTest
	public void creativeAndSpectatorUpdatesAreExempt (GameTestHelper helper) {
		ServerPlayer creative = join(helper, GameType.CREATIVE);
		ServerPlayer spectator = join(helper, GameType.SPECTATOR);
		try {
			runtime().getThirstGameplay().tick(creative);
			runtime().getThirstGameplay().tick(spectator);
			near(helper, thirst().get(creative).getHydration(), 1.0);
			near(helper, thirst().get(spectator).getHydration(), 1.0);
			final boolean[] changed = { false };
			thirst().update(creative, state -> {
				changed[0] = true;
				return new ThirstState(0.1);
			});
			thirst().update(spectator, state -> {
				changed[0] = true;
				return new ThirstState(0.1);
			});
			helper.assertTrue(!changed[0], "Exempt players must not run gameplay updates");
		} finally {
			leave(creative);
			leave(spectator);
		}
		helper.succeed();
	}

	@GameTest
	public void synchronizationOnlySendsVisibleChangesToTheirOwnerAndRefreshesLifecycleState (GameTestHelper helper) {
		ServerPlayer first = join(helper, GameType.SURVIVAL);
		ServerPlayer second = join(helper, GameType.SURVIVAL);
		try {
			seed(first, 0.749);
			seed(second, 0.4);
			var transport = new RecordingTransport();
			var config = new ThirstConfig();
			var synchronizer = new ThirstSynchronizer(thirst(), config, transport);
			synchronizer.refresh(first);
			synchronizer.update(first);
			seed(first, 0.701);
			synchronizer.update(first);
			helper.assertTrue(transport.deliveries.size() == 1, "Sub-icon drain must not send duplicate snapshots");
			seed(first, 0.7);
			synchronizer.update(first);
			helper.assertTrue(transport.deliveries.size() == 2, "Crossing an icon step must send an update");
			helper.assertTrue(transport.deliveries.stream().allMatch(delivery -> delivery.player() == first), "A player's hydration must only be sent to its owner");
			synchronizer.update(second);
			helper.assertTrue(transport.deliveries.get(2).payload().getSnapshot().getHalfUnits() == 8, "The second player must receive its own hydration");
			config.getEnabled().set(false);
			synchronizer.update(first);
			helper.assertTrue(!transport.deliveries.get(3).payload().getSnapshot().isEnabled(), "Disabling thirst must hide the client's HUD");
			synchronizer.refresh(first);
			helper.assertTrue(transport.deliveries.size() == 5, "Lifecycle refresh must resend even an unchanged snapshot");
		} finally {
			leave(first);
			leave(second);
		}
		helper.succeed();
	}

	@GameTest
	public void synchronizationRetriesWhenTheConnectionWasNotReady (GameTestHelper helper) {
		ServerPlayer player = join(helper, GameType.SURVIVAL);
		try {
			var transport = new RecordingTransport();
			var synchronizer = new ThirstSynchronizer(thirst(), new ThirstConfig(), transport);
			transport.ready = false;
			synchronizer.refresh(player);
			helper.assertTrue(transport.deliveries.isEmpty(), "An unavailable connection must not be marked as synchronized");
			transport.ready = true;
			synchronizer.update(player);
			synchronizer.update(player);
			helper.assertTrue(transport.deliveries.size() == 1, "The first ready update must deliver exactly one snapshot");
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	private record Delivery(ServerPlayer player, ThirstSyncPayload payload) {
	}

	private static final class RecordingTransport implements ThirstSyncTransport {
		private final Map<ServerPlayer, ThirstSyncTracker> trackers   = new IdentityHashMap<>();
		private final List<Delivery>                       deliveries = new ArrayList<>();
		private       boolean                              ready      = true;

		@Override
		public ThirstSyncTracker tracker (ServerPlayer player) {
			return trackers.computeIfAbsent(player, ignored -> new ThirstSyncTracker());
		}

		@Override
		public boolean send (ServerPlayer player, ThirstSyncPayload payload) {
			if (!ready) return false;
			deliveries.add(new Delivery(player, payload));
			return true;
		}
	}

	private static SverveRuntime runtime () {
		return FabricLoader.getInstance().getEntrypoints("main", ModInitializer.class).stream().filter(SverveFabric.class::isInstance).map(SverveFabric.class::cast).findFirst().orElseThrow().getRuntime();
	}

	private static PlayerThirst thirst () {
		return runtime().getPlayerThirst();
	}

	private static ServerPlayer join (GameTestHelper helper, GameType gameMode) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(gameMode);
		var cookie = CommonListenerCookie.createInitial(player.getGameProfile(), false);
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		return player;
	}

	private static void leave (ServerPlayer player) {
		player.level().getServer().getPlayerList().remove(player);
	}

	private static void seed (ServerPlayer player, double hydration) {
		thirst().update(player, state -> new ThirstState(hydration));
	}

	private static void drink (ServerPlayer player, ItemStack stack, int ticks) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		player.startUsingItem(InteractionHand.MAIN_HAND);
		for (int i = 0; i < ticks; i++) player.doTick();
	}

	private static void near (GameTestHelper helper, double actual, double expected) {
		helper.assertTrue(Math.abs(actual - expected) < 1e-10, "Expected hydration " + expected + ", got " + actual);
	}
}
