package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.network.thirst.ThirstSynchronizer;
import com.iso2t.sverve.player.temperature.TemperatureMetabolism;
import com.iso2t.sverve.survival.thirst.ThirstSystem;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Server gameplay bridge. Native hooks supply ticks and completed item use, never inventory edits.
 */
@RequiredArgsConstructor
public final class ThirstGameplay {
	private static final double SECONDS_PER_TICK = 1.0 / 20.0;

	@NonNull
	private final PlayerThirst          players;
	@NonNull
	private final ThirstSystem          system;
	@NonNull
	private final ThirstSynchronizer    synchronizer;
	@NonNull
	private final ThirstPenalties       penalties;
	@NonNull
	private final TemperatureMetabolism metabolism;

	public void tick (@NonNull ServerPlayer player) {
		var state = players.update(player, current -> system.advanceActivePlay(current, player.isSprinting(), metabolism.thirstMultiplier(player), SECONDS_PER_TICK));
		penalties.tick(player, state);
		synchronizer.update(player);
	}

	/**
	 * consumed is a snapshot from before vanilla consumed the item and returned its remainder.
	 */
	public void finishUsingItem (@NonNull ServerPlayer player, @NonNull ItemStack consumed) {
		if (isPlainWaterBottle(consumed)) {
			var state = players.update(player, system::drinkWater);
			penalties.hydrationChanged(player, state);
			synchronizer.update(player);
		}
	}

	public static boolean isPlainWaterBottle (ItemStack stack) {
		return stack.is(Items.POTION) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
	}
}
