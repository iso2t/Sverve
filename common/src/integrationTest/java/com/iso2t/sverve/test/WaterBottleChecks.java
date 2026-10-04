package com.iso2t.sverve.test;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Native regression fixtures compiled only into the two loaders' test mods.
 */
@UtilityClass
public class WaterBottleChecks {
	public static void fullInventoryDrinking (ServerPlayer player) {
		withInventory(player, () -> {
			player.setGameMode(GameType.SURVIVAL);
			for (var hand : InteractionHand.values()) {
				player.getInventory().clearContent();
				fillInventory(player);
				player.setItemInHand(hand, water(16));
				var dropsBefore = new HashSet<>(drops(player));
				drink(player, hand);
				require(player.getItemInHand(hand).getCount() == 15 && isWater(player.getItemInHand(hand)), "Full inventory drinking must consume exactly one water bottle from " + hand);
				require(player.getInventory().countItem(Items.GLASS_BOTTLE) == 0, "A full inventory must not retain an extra empty bottle");
				var created = drops(player).stream().filter(item -> !dropsBefore.contains(item)).toList();
				require(created.size() == 1 && created.getFirst().getItem().is(Items.GLASS_BOTTLE) && created.getFirst().getItem().getCount() == 1, "Full inventory drinking must drop exactly one glass bottle");
				created.forEach(ItemEntity::discard);
			}
			player.getInventory().clearContent();
			fillInventory(player);
			player.setItemInHand(InteractionHand.MAIN_HAND, water(1));
			var before = new HashSet<>(drops(player));
			drink(player, InteractionHand.MAIN_HAND);
			require(player.getMainHandItem().is(Items.GLASS_BOTTLE) && player.getMainHandItem().getCount() == 1, "The last water bottle must become the glass bottle in hand");
			require(drops(player).stream().allMatch(before::contains), "The last bottle must not create a duplicate dropped remainder");
		});
	}

	public static void creativeDrinking (ServerPlayer player) {
		withInventory(player, () -> {
			player.setGameMode(GameType.CREATIVE);
			player.getInventory().clearContent();
			for (var hand : InteractionHand.values()) {
				player.setItemInHand(hand, water(16));
				drink(player, hand);
				require(isWater(player.getItemInHand(hand)) && player.getItemInHand(hand).getCount() == 16, "Creative drinking must retain the full water stack: " + hand + " result=" + player.getItemInHand(hand) + " mode=" + player.gameMode() + " infinite=" + player.hasInfiniteMaterials());
			}
			require(player.getInventory().countItem(Items.GLASS_BOTTLE) == 0, "Creative drinking must not create empty bottles");
		});
	}

	public static void brewingMenu (ServerPlayer player, BlockPos pos) {
		var level = player.level();
		var original = level.getBlockState(pos);
		try {
			level.setBlockAndUpdate(pos, Blocks.BREWING_STAND.defaultBlockState());
			var stand = (BrewingStandBlockEntity) level.getBlockEntity(pos);
			withInventory(player, () -> {
				player.setGameMode(GameType.SURVIVAL);
				player.getInventory().clearContent();
				player.setItemInHand(InteractionHand.MAIN_HAND, water(16));
				var menu = stand.createMenu(0, player.getInventory(), player);
				menu.clicked(32 + player.getInventory().getSelectedSlot(), 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, player);
				require(player.getMainHandItem().getCount() == 13, "Shift-click must leave thirteen water bottles after filling three brewing slots");
				for (int slot = 0; slot < 3; slot++) {
					require(menu.getSlot(slot).getMaxStackSize(water(16)) == 1 && stand.getItem(slot).getCount() == 1 && isWater(stand.getItem(slot)), "Each brewing slot must contain one water bottle");
				}
				brew(level, pos, stand);
				for (int slot = 0; slot < 3; slot++) require(isAwkward(stand.getItem(slot)) && stand.getItem(slot).getCount() == 1 && stand.getItem(slot).getMaxStackSize() == 1, "Brewing must produce three separate unstackable potions");
				for (int slot = 0; slot < 3; slot++) menu.quickMoveStack(player, slot);
				require(player.getInventory().countItem(Items.POTION) == 16, "Brewing and collecting must preserve all sixteen bottles");
			});
		} finally {
			if (level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand) stand.clearContent();
			level.setBlockAndUpdate(pos, original);
		}
	}

	public static void hoppersAndBrewing (ServerLevel level, BlockPos pos) {
		var sourcePos = pos.west();
		var sinkPos = pos.below();
		var originalStand = level.getBlockState(pos);
		var originalSource = level.getBlockState(sourcePos);
		var originalSink = level.getBlockState(sinkPos);
		try {
			level.setBlockAndUpdate(pos, Blocks.BREWING_STAND.defaultBlockState());
			level.setBlockAndUpdate(sourcePos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
			level.setBlockAndUpdate(sinkPos, Blocks.HOPPER.defaultBlockState());
			var stand = (BrewingStandBlockEntity) level.getBlockEntity(pos);
			var source = (HopperBlockEntity) level.getBlockEntity(sourcePos);
			var sink = (HopperBlockEntity) level.getBlockEntity(sinkPos);
			source.setItem(0, water(16));
			for (int tick = 0; tick < 40; tick++) HopperBlockEntity.pushItemsTick(level, sourcePos, level.getBlockState(sourcePos), source);
			for (int slot = 0; slot < 3; slot++) require(stand.getItem(slot).getCount() == 1 && isWater(stand.getItem(slot)), "Hoppers must insert one bottle per brewing slot");
			require(source.getItem(0).getCount() == 13, "A full brewing stand must leave thirteen bottles in its source hopper");
			brew(level, pos, stand);
			for (int tick = 0; tick < 40; tick++) HopperBlockEntity.pushItemsTick(level, sinkPos, level.getBlockState(sinkPos), sink);
			require(stand.isEmpty() && sink.countItem(Items.POTION) == 3 && source.getItem(0).getCount() == 13, "Extraction must preserve every bottle");
			for (int slot = 0; slot < 3; slot++) require(isAwkward(sink.getItem(slot)) && sink.getItem(slot).getCount() == 1, "Hoppers must preserve unstackable brewed contents");
			// Ordinary containers still merge water up to sixteen.
			sink.clearContent();
			sink.setItem(0, water(15));
			var remainder = HopperBlockEntity.addItem(null, sink, water(3), Direction.UP);
			require(remainder.isEmpty() && sink.getItem(0).getCount() == 16 && sink.getItem(1).getCount() == 2, "Hoppers must split water overflow at sixteen");
		} finally {
			for (var blockPos : new BlockPos[] { pos, sourcePos, sinkPos }) if (level.getBlockEntity(blockPos) instanceof net.minecraft.world.Container container) container.clearContent();
			level.setBlockAndUpdate(pos, originalStand);
			level.setBlockAndUpdate(sourcePos, originalSource);
			level.setBlockAndUpdate(sinkPos, originalSink);
		}
	}

	public static void dispensers (ServerLevel level, BlockPos pos) {
		var target = pos.east();
		var original = level.getBlockState(pos);
		var originalTarget = level.getBlockState(target);
		var area = new AABB(pos).inflate(3);
		var originalDrops = new HashSet<>(level.getEntitiesOfClass(ItemEntity.class, area));
		try {
			level.setBlockAndUpdate(pos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
			var dispenser = (DispenserBlockEntity) level.getBlockEntity(pos);
			var source = new BlockSource(level, pos, level.getBlockState(pos), dispenser);
			level.setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());
			dispenser.setItem(0, water(16));
			dispense(source, 0);
			require(level.getBlockState(target).is(Blocks.MUD) && isWater(dispenser.getItem(0)) && dispenser.getItem(0).getCount() == 15 && dispenser.countItem(Items.GLASS_BOTTLE) == 1, "Dispenser mud conversion must consume one water and retain one remainder");
			for (int slot = 0; slot < dispenser.getContainerSize(); slot++) dispenser.setItem(slot, Items.STONE.getDefaultInstance().copyWithCount(64));
			dispenser.setItem(0, water(16));
			level.setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());
			dispense(source, 0);
			var dropped = level.getEntitiesOfClass(ItemEntity.class, area).stream().filter(item -> !originalDrops.contains(item)).toList();
			require(dispenser.getItem(0).getCount() == 15 && dropped.size() == 1 && dropped.getFirst().getItem().is(Items.GLASS_BOTTLE) && dropped.getFirst().getItem().getCount() == 1, "A full dispenser must drop exactly one empty bottle");
			dropped.forEach(ItemEntity::discard);
			dispenser.clearContent();
			level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
			dispenser.setItem(0, water(16));
			dispense(source, 0);
			dropped = level.getEntitiesOfClass(ItemEntity.class, area).stream().filter(item -> !originalDrops.contains(item)).toList();
			require(dispenser.getItem(0).getCount() == 15 && dropped.size() == 1 && isWater(dropped.getFirst().getItem()) && dropped.getFirst().getItem().getCount() == 1, "Dispenser fallback must eject one water bottle");
			dropped.forEach(ItemEntity::discard);
			dispenser.clearContent();
			level.setBlockAndUpdate(target, Blocks.WATER.defaultBlockState());
			dispenser.setItem(0, Items.GLASS_BOTTLE.getDefaultInstance().copyWithCount(2));
			dispenser.setItem(1, water(15));
			dispense(source, 0);
			require(dispenser.getItem(0).is(Items.GLASS_BOTTLE) && dispenser.getItem(0).getCount() == 1 && dispenser.getItem(1).getCount() == 16, "Dispensers must fill one bottle and merge water up to sixteen");
		} finally {
			if (level.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser) dispenser.clearContent();
			level.getEntitiesOfClass(ItemEntity.class, area).stream().filter(item -> !originalDrops.contains(item)).forEach(ItemEntity::discard);
			level.setBlockAndUpdate(pos, original);
			level.setBlockAndUpdate(target, originalTarget);
		}
	}

	private static void brew (ServerLevel level, BlockPos pos, BrewingStandBlockEntity stand) {
		stand.setItem(3, Items.NETHER_WART.getDefaultInstance());
		stand.setItem(4, Items.BLAZE_POWDER.getDefaultInstance());
		for (int tick = 0; tick <= BrewingStandBlockEntity.BREWING_TIME_SECONDS * 20; tick++) BrewingStandBlockEntity.serverTick(level, pos, level.getBlockState(pos), stand);
		// Fuel is consumed once; unused fuel must not affect extraction assertions.
		stand.setItem(4, ItemStack.EMPTY);
	}

	private static void dispense (BlockSource source, int slot) {
		var stack = source.blockEntity().getItem(slot);
		source.blockEntity().setItem(slot, DispenserBlock.DISPENSER_REGISTRY.get(stack.getItem()).dispense(source, stack));
	}

	private static void drink (ServerPlayer player, InteractionHand hand) {
		player.startUsingItem(hand);
		for (int tick = 0; tick < 32; tick++) player.doTick();
		require(!player.isUsingItem(), "The water bottle use must finish after thirty-two ticks");
	}

	private static void fillInventory (ServerPlayer player) {
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) player.getInventory().setItem(slot, Items.STONE.getDefaultInstance().copyWithCount(64));
	}

	private static java.util.List<ItemEntity> drops (ServerPlayer player) {
		return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8));
	}

	private static void withInventory (ServerPlayer player, Runnable action) {
		var inventory = player.getInventory();
		var saved = new ArrayList<ItemStack>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) saved.add(inventory.getItem(slot).copy());
		var mode = player.gameMode();
		var existingDrops = new HashSet<>(drops(player));
		try {
			action.run();
		} finally {
			player.releaseUsingItem();
			player.setGameMode(mode);
			inventory.clearContent();
			for (int slot = 0; slot < saved.size(); slot++) inventory.setItem(slot, saved.get(slot));
			drops(player).stream().filter(item -> !existingDrops.contains(item)).forEach(ItemEntity::discard);
		}
	}

	private static ItemStack water (int count) {
		return Items.POTION.getDefaultInstance().copyWithCount(count);
	}

	private static boolean isWater (ItemStack stack) {
		return stack.is(Items.POTION) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
	}

	private static boolean isAwkward (ItemStack stack) {
		return stack.is(Items.POTION) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.AWKWARD);
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
