package com.iso2t.sverve;

import com.iso2t.sverve.player.temperature.TemperatureDamage;
import com.iso2t.sverve.player.temperature.TemperatureProtection;
import com.iso2t.sverve.survival.temperature.TemperatureBand;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Real registry definitions, equipped enchantments, effects, damage, and live coordinator checks.
 */
public final class TemperaturePenaltyGameTests {
	private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

	@GameTest
	public void enchantmentsSupportAllArmorAndVanillaAcquisitionButExcludeEachOther (GameTestHelper helper) {
		var player = join(helper);
		try {
			var keys = List.of(TemperatureProtection.INSULATION, TemperatureProtection.HEAT_PROTECTION, TemperatureProtection.THERMAL_PROTECTION);
			for (var key : keys) {
				var enchantment = enchantment(player, key);
				helper.assertTrue(enchantment.value().getMaxLevel() == 4, "Every temperature enchantment must support I-IV");
				for (var slot : ARMOR) {
					helper.assertTrue(enchantment.value().matchingSlot(slot) && enchantment.value().canEnchant(armor(slot)), "Every armor slot must accept " + key);
				}
				helper.assertTrue(!enchantment.value().canEnchant(Items.DIAMOND_SWORD.getDefaultInstance()), "Weapons must be excluded");
				for (String tag : List.of("in_enchanting_table", "tradeable", "on_random_loot")) {
					helper.assertTrue(enchantment.is(TagKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace(tag))), "Vanilla acquisition must include " + key + " via " + tag);
				}
				var book = EnchantmentHelper.enchantItem(RandomSource.create(1), Items.BOOK.getDefaultInstance(), 30, Stream.of(enchantment));
				helper.assertTrue(book.is(Items.ENCHANTED_BOOK), "Vanilla enchanting must produce an enchanted book");
				helper.assertTrue(Enchantment.areCompatible(enchantment, enchantment(player, Enchantments.PROTECTION)), "Temperature enchantments must coexist with vanilla Protection");
				for (var other : keys) {
					if (key != other) helper.assertTrue(!Enchantment.areCompatible(enchantment, enchantment(player, other)), "Temperature enchantments must be mutually exclusive on one piece");
				}
			}
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void levelsStackAcrossWornArmorAndHeldOrInventoryItemsNeverContribute (GameTestHelper helper) {
		var player = join(helper);
		try {
			setArmor(player, TemperatureProtection.INSULATION, 1);
			near(helper, TemperatureProtection.sample(player).getColdReduction(), 0.25);
			setArmor(player, TemperatureProtection.INSULATION, 4);
			near(helper, TemperatureProtection.sample(player).getColdReduction(), 1);
			setArmor(player, TemperatureProtection.THERMAL_PROTECTION, 4);
			near(helper, TemperatureProtection.sample(player).getColdReduction(), 0.5);
			near(helper, TemperatureProtection.sample(player).getHeatReduction(), 0.5);
			equip(player, EquipmentSlot.HEAD, TemperatureProtection.INSULATION, 4);
			equip(player, EquipmentSlot.CHEST, TemperatureProtection.INSULATION, 4);
			near(helper, TemperatureProtection.sample(player).getColdReduction(), 0.75);
			near(helper, TemperatureProtection.sample(player).getHeatReduction(), 0.25);
			clearArmor(player);
			var spare = armor(EquipmentSlot.HEAD);
			spare.enchant(enchantment(player, TemperatureProtection.INSULATION), 4);
			player.setItemInHand(InteractionHand.MAIN_HAND, spare.copy());
			player.setItemInHand(InteractionHand.OFF_HAND, spare.copy());
			player.getInventory().add(spare.copy());
			near(helper, TemperatureProtection.sample(player).getColdReduction(), 0);
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void onlyExtremesDamageAndVanillaArmorDoesNotChangeTheProtectionFormula (GameTestHelper helper) {
		var player = join(helper);
		try {
			for (double exposure : new double[] { -0.749, 0, 0.749 }) {
				seed(player, exposure);
				ticks(player, 160);
				health(helper, player, 20);
			}
			setArmor(player, Enchantments.PROTECTION, 4);
			player.doTick();
			helper.assertTrue(player.getArmorValue() == 20, "Fixture must have full diamond armor");
			for (var band : new TemperatureBand[] { TemperatureBand.FREEZING, TemperatureBand.HOT }) {
				var source = TemperatureDamage.source(player, band);
				helper.assertTrue(source.is(DamageTypeTags.BYPASSES_ARMOR) && source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS) && source.is(DamageTypeTags.NO_KNOCKBACK), "Temperature damage must use its own protection policy");
				helper.assertTrue(!source.scalesWithDifficulty(), "Difficulty must not change temperature damage");
				seed(player, band == TemperatureBand.FREEZING ? -0.75 : 0.75);
				player.setHealth(20);
				player.damageCooldownTime = 0;
				ticks(player, 79);
				health(helper, player, 20);
				ticks(player, 1);
				health(helper, player, 18);
				helper.assertTrue(player.getLastDamageSource().is(band == TemperatureBand.FREEZING ? TemperatureDamage.FREEZING : TemperatureDamage.OVERHEATING), "The correct custom damage type must be used");
				player.damageCooldownTime = 0;
				ticks(player, 80);
				health(helper, player, 16);
			}
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void armorSwapsImmediatelyScaleDamageAndCompleteProtectionPreventsIt (GameTestHelper helper) {
		var player = join(helper);
		try {
			seed(player, -1);
			ticks(player, 79);
			equip(player, EquipmentSlot.HEAD, TemperatureProtection.INSULATION, 4);
			ticks(player, 1);
			health(helper, player, 18.5f);
			player.damageCooldownTime = 0;
			setArmor(player, TemperatureProtection.INSULATION, 4);
			ticks(player, 160);
			health(helper, player, 18.5f);
			// Breaking/removing all worn pieces immediately removes immunity and starts a fresh interval.
			clearArmor(player);
			ticks(player, 79);
			health(helper, player, 18.5f);
			ticks(player, 1);
			health(helper, player, 16.5f);
			player.damageCooldownTime = 0;
			setArmor(player, TemperatureProtection.THERMAL_PROTECTION, 4);
			ticks(player, 80);
			health(helper, player, 15.5f);
			seed(player, 1);
			player.damageCooldownTime = 0;
			ticks(player, 80);
			health(helper, player, 14.5f);
			setArmor(player, TemperatureProtection.HEAT_PROTECTION, 4);
			ticks(player, 160);
			health(helper, player, 14.5f);
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void heatDrainsVanillaHungerAndThirstWithoutApplyingStatusEffects (GameTestHelper helper) {
		var player = join(helper);
		var config = runtime().getConfig().getTemperature();
		var rate = config.getResponseRate().get();
		var thirstConfig = runtime().getConfig().getThirst();
		var dryAirLoss = thirstConfig.getDryAirLoss().get();
		try {
			config.getResponseRate().set(0.0);
			thirstConfig.getDryAirLoss().set(0.0);
			seed(player, 1);
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000));
			player.getFoodData().setFoodLevel(20);
			player.getFoodData().setSaturation(0);
			double hydration = runtime().getPlayerThirst().get(player).getHydration();
			for (int tick = 0; tick < 420; tick++) {
				runtime().getTemperatureGameplay().tick(player);
				runtime().getThirstGameplay().tick(player);
				player.getFoodData().tick(player);
			}
			health(helper, player, 20);
			helper.assertTrue(player.getFoodData().getFoodLevel() == 19, "Heat must spend vanilla exhaustion and drain hunger");
			near(helper, runtime().getPlayerThirst().get(player).getHydration(), hydration - (thirstConfig.getBaseLoss().get() + thirstConfig.getHeatLoss().get()) * 21);
			helper.assertTrue(player.getActiveEffects().size() == 1 && player.hasEffect(MobEffects.FIRE_RESISTANCE), "Temperature must preserve vanilla Fire Resistance without adding effects");
			config.getMetabolismEnabled().set(false);
			hydration = runtime().getPlayerThirst().get(player).getHydration();
			runtime().getThirstGameplay().tick(player);
			near(helper, runtime().getPlayerThirst().get(player).getHydration(), hydration - 1.0 / 24000);
			player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 500, 2));
			config.getMetabolismEnabled().set(true);
			ticks(player, 1);
			helper.assertTrue(player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier() == 2, "Temperature gameplay must preserve unrelated effects");
		} finally {
			config.getMetabolismEnabled().set(true);
			config.getResponseRate().set(rate);
			thirstConfig.getDryAirLoss().set(dryAirLoss);
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void armorSwapsImmediatelyScaleMetabolismAndPreserveBaselineThirst (GameTestHelper helper) {
		var player = join(helper);
		var metabolism = runtime().getTemperatureMetabolism();
		var thirstConfig = runtime().getConfig().getThirst();
		double dryAirLoss = thirstConfig.getDryAirLoss().get();
		try {
			seed(player, 1);
			setArmor(player, TemperatureProtection.THERMAL_PROTECTION, 4);
			near(helper, metabolism.thirstHeat(player), 0.5);
			resetFood(player);
			for (int tick = 0; tick < 20; tick++) {
				metabolism.tick(player, new TemperatureState(1));
				player.getFoodData().tick(player);
			}
			helper.assertTrue(player.getFoodData().getFoodLevel() == 19, "Partial protection must retain some heat exhaustion");
			setArmor(player, TemperatureProtection.HEAT_PROTECTION, 4);
			near(helper, metabolism.thirstHeat(player), 0);
			resetFood(player);
			for (int tick = 0; tick < 20; tick++) {
				metabolism.tick(player, new TemperatureState(1));
				player.getFoodData().tick(player);
			}
			helper.assertTrue(player.getFoodData().getFoodLevel() == 20, "Full protection must remove heat exhaustion");
			thirstConfig.getDryAirLoss().set(0.0);
			runtime().getPlayerThirst().update(player, ignored -> new com.iso2t.sverve.survival.thirst.ThirstState(0.8));
			player.setSprinting(true);
			for (int tick = 0; tick < 20; tick++) runtime().getThirstGameplay().tick(player);
			near(helper, runtime().getPlayerThirst().get(player).getHydration(), 0.8 - thirstConfig.getBaseLoss().get() - thirstConfig.getSprintLoss().get());
			seed(player, -1);
			helper.assertTrue(metabolism.healingInterval(player, 80) == 160, "Heat armor must not protect cold healing");
			setArmor(player, TemperatureProtection.THERMAL_PROTECTION, 4);
			helper.assertTrue(metabolism.healingInterval(player, 80) == 120 && metabolism.healingInterval(player, 10) == 15, "Universal protection must halve extra cold healing delay");
			setArmor(player, TemperatureProtection.INSULATION, 4);
			helper.assertTrue(metabolism.healingInterval(player, 80) == 80 && metabolism.healingInterval(player, 10) == 10, "Full cold protection must restore vanilla healing cadence");
			seed(player, 1);
			near(helper, metabolism.thirstHeat(player), 1);
			clearArmor(player);
			near(helper, metabolism.thirstHeat(player), 1);
			helper.assertTrue(player.getActiveEffects().isEmpty(), "Protection must not create potion effects");
		} finally {
			thirstConfig.getDryAirLoss().set(dryAirLoss);
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void fireResistanceBlocksOnlyHeatDamageAndExpiryStartsFreshGraceTime (GameTestHelper helper) {
		var player = join(helper);
		try {
			helper.assertTrue(TemperatureDamage.source(player, TemperatureBand.HOT).is(DamageTypeTags.IS_FIRE), "Overheating must use vanilla fire immunity");
			helper.assertTrue(!TemperatureDamage.source(player, TemperatureBand.FREEZING).is(DamageTypeTags.IS_FIRE), "Freezing must remain independent of fire immunity");
			seed(player, 1);
			ticks(player, 79);
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000));
			ticks(player, 81);
			health(helper, player, 20);
			near(helper, runtime().getTemperatureMetabolism().thirstHeat(player), 1);
			player.removeEffect(MobEffects.FIRE_RESISTANCE);
			ticks(player, 79);
			health(helper, player, 20);
			ticks(player, 1);
			health(helper, player, 18);
			player.damageCooldownTime = 0;
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000));
			seed(player, -1);
			ticks(player, 80);
			health(helper, player, 16);
			helper.assertTrue(player.hasEffect(MobEffects.FIRE_RESISTANCE), "Temperature must preserve external effects");
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void coldSlowsBothVanillaNaturalHealingPathsWithoutChangingHealingAmount (GameTestHelper helper) {
		var player = join(helper);
		var rules = helper.getLevel().getGameRules();
		boolean originalRule = rules.get(GameRules.NATURAL_HEALTH_REGENERATION);
		try {
			rules.set(GameRules.NATURAL_HEALTH_REGENERATION, true, helper.getLevel().getServer());
			for (double exposure : new double[] { 0, -0.5, -1 }) {
				seed(player, exposure);
				for (boolean saturated : new boolean[] { false, true }) {
					var food = new FoodData();
					((com.iso2t.sverve.player.temperature.TemperatureFoodAccess) food).sverve$setTemperatureMetabolism(runtime().getTemperatureMetabolism());
					food.setFoodLevel(saturated ? 20 : 18);
					food.setSaturation(saturated ? 6 : 0);
					player.setHealth(10);
					int interval = saturated ? (exposure == 0 ? 10 : exposure == -0.5 ? 15 : 20) : (exposure == 0 ? 80 : exposure == -0.5 ? 120 : 160);
					for (int tick = 0; tick < interval - 1; tick++) food.tick(player);
					health(helper, player, 10);
					food.tick(player);
					health(helper, player, 11);
				}
			}
			player.heal(2);
			health(helper, player, 13);
			helper.assertTrue(player.getActiveEffects().isEmpty(), "Cold must not apply a status effect");
			rules.set(GameRules.NATURAL_HEALTH_REGENERATION, false, helper.getLevel().getServer());
			var food = new FoodData();
			((com.iso2t.sverve.player.temperature.TemperatureFoodAccess) food).sverve$setTemperatureMetabolism(runtime().getTemperatureMetabolism());
			food.setFoodLevel(20);
			for (int tick = 0; tick < 200; tick++) food.tick(player);
			health(helper, player, 13);
		} finally {
			rules.set(GameRules.NATURAL_HEALTH_REGENERATION, originalRule, helper.getLevel().getServer());
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void coldLeavesStarvationTimingAndExemptOrDisabledHealingUnchanged (GameTestHelper helper) {
		var player = join(helper);
		var config = runtime().getConfig().getTemperature();
		try {
			seed(player, -1);
			var food = new FoodData();
			((com.iso2t.sverve.player.temperature.TemperatureFoodAccess) food).sverve$setTemperatureMetabolism(runtime().getTemperatureMetabolism());
			food.setFoodLevel(0);
			food.setSaturation(0);
			player.setHealth(20);
			for (int tick = 0; tick < 79; tick++) food.tick(player);
			health(helper, player, 20);
			food.tick(player);
			health(helper, player, 19);
			for (var mode : new GameType[] { GameType.CREATIVE, GameType.SPECTATOR }) {
				player.setGameMode(mode);
				helper.assertTrue(runtime().getTemperatureMetabolism().healingInterval(player, 80) == 80, "Exempt modes must use the vanilla healing interval");
				near(helper, runtime().getTemperatureMetabolism().thirstHeat(player), 0);
			}
			player.setGameMode(GameType.SURVIVAL);
			config.getEnabled().set(false);
			helper.assertTrue(runtime().getTemperatureMetabolism().healingInterval(player, 80) == 80, "Disabled temperature must use vanilla healing");
		} finally {
			config.getEnabled().set(true);
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void recoveryModesConfigAndSwitchingExtremesResetDamageTiming (GameTestHelper helper) {
		var player = join(helper);
		var config = runtime().getConfig().getTemperature();
		try {
			seed(player, -1);
			ticks(player, 79);
			seed(player, 0);
			ticks(player, 1);
			seed(player, -1);
			ticks(player, 79);
			health(helper, player, 20);
			seed(player, 1);
			ticks(player, 79);
			health(helper, player, 20);
			for (var mode : new GameType[] { GameType.CREATIVE, GameType.SPECTATOR }) {
				player.setGameMode(mode);
				ticks(player, 100);
				health(helper, player, 20);
			}
			player.setGameMode(GameType.ADVENTURE);
			config.getEnabled().set(false);
			ticks(player, 100);
			config.getEnabled().set(true);
			ticks(player, 79);
			health(helper, player, 20);
			config.getOverheatingDamage().set(0.0);
			ticks(player, 100);
			health(helper, player, 20);
			config.getOverheatingDamage().set(3.0);
			config.getDamageIntervalSeconds().set(3);
			ticks(player, 59);
			health(helper, player, 20);
			ticks(player, 1);
			health(helper, player, 17);
		} finally {
			config.getEnabled().set(true);
			config.getOverheatingDamage().set(2.0);
			config.getDamageIntervalSeconds().set(4);
			leave(player);
		}
		helper.succeed();
	}

	@GameTest
	public void extremeDamageCanKillAndPlayerTimersRemainIndependent (GameTestHelper helper) {
		var first = join(helper);
		var second = join(helper);
		try {
			seed(first, -1);
			seed(second, 1);
			first.setHealth(2);
			second.setHealth(2);
			ticks(first, 79);
			ticks(second, 80);
			helper.assertTrue(!second.isAlive() && second.getLastDamageSource().is(TemperatureDamage.OVERHEATING), "Overheating must be lethal with its custom damage source");
			health(helper, first, 2);
			ticks(first, 1);
			helper.assertTrue(!first.isAlive() && first.getLastDamageSource().is(TemperatureDamage.FREEZING), "Freezing must use its own independent lethal pulse");
			ticks(first, 100);
			health(helper, first, 0);
		} finally {
			leave(first);
			leave(second);
		}
		helper.succeed();
	}

	private static SverveRuntime runtime () {
		return FabricLoader.getInstance().getEntrypoints("main", ModInitializer.class).stream().filter(SverveFabric.class::isInstance).map(SverveFabric.class::cast).findFirst().orElseThrow().getRuntime();
	}

	private static Holder.Reference<Enchantment> enchantment (ServerPlayer player, ResourceKey<Enchantment> key) {
		return player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	private static ServerPlayer join (GameTestHelper helper) {
		var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "temperature-penalty"), ClientInformation.createDefault());
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
		return player;
	}

	private static ItemStack armor (EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> Items.DIAMOND_HELMET.getDefaultInstance();
			case CHEST -> Items.DIAMOND_CHESTPLATE.getDefaultInstance();
			case LEGS -> Items.DIAMOND_LEGGINGS.getDefaultInstance();
			case FEET -> Items.DIAMOND_BOOTS.getDefaultInstance();
			default -> throw new IllegalArgumentException("Not an armor slot");
		};
	}

	private static void equip (ServerPlayer player, EquipmentSlot slot, ResourceKey<Enchantment> key, int level) {
		var piece = armor(slot);
		piece.enchant(enchantment(player, key), level);
		player.setItemSlot(slot, piece);
	}

	private static void setArmor (ServerPlayer player, ResourceKey<Enchantment> key, int level) {
		for (var slot : ARMOR) equip(player, slot, key, level);
	}

	private static void clearArmor (ServerPlayer player) {
		for (var slot : ARMOR) player.setItemSlot(slot, ItemStack.EMPTY);
	}

	private static void resetFood (ServerPlayer player) {
		var tag = new CompoundTag();
		tag.putInt("foodLevel", 20);
		tag.putFloat("foodSaturationLevel", 0);
		tag.putFloat("foodExhaustionLevel", 3.95f);
		tag.putInt("foodTickTimer", 0);
		player.getFoodData().readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), tag));
	}

	private static void seed (ServerPlayer player, double exposure) {
		runtime().getPlayerTemperature().update(player, ignored -> new TemperatureState(exposure));
	}

	private static void ticks (ServerPlayer player, int count) {
		// Freeze biome response so assertions isolate penalties while still exercising the real coordinator.
		var rate = runtime().getConfig().getTemperature().getResponseRate();
		double original = rate.get();
		try {
			rate.set(0.0);
			for (int tick = 0; tick < count; tick++) runtime().getTemperatureGameplay().tick(player);
		} finally {
			rate.set(original);
		}
	}

	private static void health (GameTestHelper helper, ServerPlayer player, float expected) {
		near(helper, player.getHealth(), expected);
	}

	private static void near (GameTestHelper helper, double actual, double expected) {
		helper.assertTrue(Math.abs(actual - expected) < 1e-6, "Expected " + expected + ", got " + actual);
	}

	private static void leave (ServerPlayer player) {
		player.level().getServer().getPlayerList().remove(player);
	}
}
