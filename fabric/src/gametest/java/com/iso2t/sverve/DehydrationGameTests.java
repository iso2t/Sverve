package com.iso2t.sverve;

import com.iso2t.sverve.player.thirst.DehydrationDamage;
import com.iso2t.sverve.survival.thirst.ThirstState;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Real server damage/tag, completed drinking, and player-replacement checks.
 */
public final class DehydrationGameTests {
	@GameTest
	public void damageWaitsFourSecondsBypassesArmorAndRepeats (GameTestHelper helper) {
		var player = join(helper);
		try {
			player.setItemSlot(EquipmentSlot.HEAD, Items.DIAMOND_HELMET.getDefaultInstance());
			player.setItemSlot(EquipmentSlot.CHEST, Items.DIAMOND_CHESTPLATE.getDefaultInstance());
			player.setItemSlot(EquipmentSlot.LEGS, Items.DIAMOND_LEGGINGS.getDefaultInstance());
			player.setItemSlot(EquipmentSlot.FEET, Items.DIAMOND_BOOTS.getDefaultInstance());
			player.doTick();
			helper.assertTrue(player.getArmorValue() == 20, "The fixture must apply the equipped diamond armor attributes");
			var source = DehydrationDamage.source(player);
			helper.assertTrue(source.is(DamageTypeTags.BYPASSES_ARMOR), "Dehydration must bypass armor");
			helper.assertTrue(source.is(DamageTypeTags.BYPASSES_SHIELD), "Armor bypass must also bypass shields");
			helper.assertTrue(source.is(DamageTypeTags.NO_KNOCKBACK), "Dehydration must not knock the player back");
			helper.assertTrue(!source.scalesWithDifficulty(), "Difficulty must not reduce dehydration damage");
			ticks(player, 79);
			health(helper, player, 20);
			ticks(player, 1);
			health(helper, player, 18);
			helper.assertTrue(player.getLastDamageSource().is(DehydrationDamage.TYPE), "The pulse must use the custom damage type");
			// These checks advance the gameplay hook directly, not vanilla's entity immunity clock.
			player.damageCooldownTime = 0;
			ticks(player, 79);
			health(helper, player, 18);
			ticks(player, 1);
			health(helper, player, 16);
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void completedDrinkingResetsTheDamageTimer (GameTestHelper helper) {
		var player = join(helper);
		try {
			ticks(player, 79);
			player.setItemInHand(InteractionHand.MAIN_HAND, Items.POTION.getDefaultInstance());
			player.startUsingItem(InteractionHand.MAIN_HAND);
			for (int i = 0; i < 32; i++) player.doTick();
			helper.assertTrue(runtime().getPlayerThirst().get(player).getHydration() > 0, "Completed vanilla water use must hydrate the player");
			helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "Vanilla must retain its bottle remainder");
			// Empty again before another gameplay tick: completion itself must reset the old timer.
			seed(player, 0);
			ticks(player, 79);
			health(helper, player, 20);
			ticks(player, 1);
			health(helper, player, 18);
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void disabledThirstAndZeroDamagePauseTheTimerAndLiveSettingsApply (GameTestHelper helper) {
		var player = join(helper);
		var config = runtime().getConfig().getThirst();
		try {
			ticks(player, 40);
			config.getEnabled().set(false);
			ticks(player, 200);
			health(helper, player, 20);
			config.getEnabled().set(true);
			config.getDehydrationDamage().set(0.0);
			ticks(player, 200);
			health(helper, player, 20);
			config.getDehydrationDamage().set(3.0);
			config.getDehydrationIntervalSeconds().set(3);
			ticks(player, 19);
			health(helper, player, 20);
			ticks(player, 1);
			health(helper, player, 17);
		} finally {
			config.getEnabled().set(true);
			config.getDehydrationDamage().set(2.0);
			config.getDehydrationIntervalSeconds().set(4);
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void exemptPlayersResetTimingAndPlayersHaveIndependentPulses (GameTestHelper helper) {
		var first = join(helper);
		var second = join(helper);
		try {
			ticks(first, 79);
			for (var mode : new GameType[] { GameType.CREATIVE, GameType.SPECTATOR }) {
				first.setGameMode(mode);
				ticks(first, 200);
				health(helper, first, 20);
			}
			first.setGameMode(GameType.ADVENTURE);
			ticks(first, 79);
			health(helper, first, 20);
			ticks(second, 80);
			health(helper, second, 18);
			health(helper, first, 20);
			ticks(first, 1);
			health(helper, first, 18);
			first.setHealth(0);
			ticks(first, 100);
			health(helper, first, 0);
		} finally {
			leave(first);
			leave(second);
		}
		helper.succeed();
	}

	@GameTest
	public void dehydrationCanKillAndDeathRespawnResetsWithEitherKeepInventorySetting (GameTestHelper helper) {
		var rules = helper.getLevel().getGameRules();
		boolean originalKeepInventory = rules.get(GameRules.KEEP_INVENTORY);
		try {
			for (boolean keepInventory : new boolean[] { false, true }) {
				rules.set(GameRules.KEEP_INVENTORY, keepInventory, helper.getLevel().getServer());
				var player = join(helper);
				try {
					player.setHealth(2);
					ticks(player, 80);
					helper.assertTrue(!player.isAlive(), "Dehydration must be lethal even with keepInventory=" + keepInventory);
					helper.assertTrue(player.getLastDamageSource().getLocalizedDeathMessage(player).getContents() instanceof TranslatableContents message && message.getKey().equals("death.attack.sverve.dehydration"), "Dehydration must send the custom translatable death message");
					player = helper.getLevel().getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
					player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
					helper.assertTrue(runtime().getPlayerThirst().get(player).getHydration() == 1, "Death respawn must start fully hydrated");
					ticks(player, 80);
					health(helper, player, 20);
				} finally {
					leave(player);
				}
			}
		} finally {
			rules.set(GameRules.KEEP_INVENTORY, originalKeepInventory, helper.getLevel().getServer());
		}
		helper.succeed();
	}

	private static SverveRuntime runtime () {
		return FabricLoader.getInstance().getEntrypoints("main", ModInitializer.class).stream().filter(SverveFabric.class::isInstance).map(SverveFabric.class::cast).findFirst().orElseThrow().getRuntime();
	}

	private static ServerPlayer join (GameTestHelper helper) {
		// Vanilla mock players hard-code their game mode; use a real player and acknowledge loading.
		var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "dehydration-test"), ClientInformation.createDefault());
		var connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
		player.setGameMode(GameType.SURVIVAL);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 10, 0))));
		player.setPermanentlyInvulnerable(false);
		player.setInvulnerableTime(0);
		player.setHealth(20);
		player.getFoodData().setFoodLevel(10);
		seed(player, 0);
		return player;
	}

	private static void seed (ServerPlayer player, double hydration) {
		runtime().getPlayerThirst().update(player, current -> new ThirstState(hydration));
	}

	private static void ticks (ServerPlayer player, int count) {
		for (int tick = 0; tick < count; tick++) runtime().getThirstGameplay().tick(player);
	}

	private static void health (GameTestHelper helper, ServerPlayer player, float expected) {
		helper.assertTrue(Math.abs(player.getHealth() - expected) < 1e-6, "Expected health " + expected + ", got " + player.getHealth());
	}

	private static void leave (ServerPlayer player) {
		player.level().getServer().getPlayerList().remove(player);
	}
}
