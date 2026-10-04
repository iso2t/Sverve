package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.item.WaterBottles;
import com.iso2t.sverve.network.thirst.ThirstSynchronizer;
import com.iso2t.sverve.player.environment.BiomeEnvironmentSampler;
import com.iso2t.sverve.player.exertion.LoadExertion;
import com.iso2t.sverve.player.temperature.TemperatureMetabolism;
import com.iso2t.sverve.survival.thirst.ThirstSystem;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Server gameplay bridge. Native hooks supply ticks and completed item use, never inventory edits.
 */
@RequiredArgsConstructor
public final class ThirstGameplay {
	private static final double SECONDS_PER_TICK = 1.0 / 20.0;

	@NonNull
	private final PlayerThirst            players;
	@NonNull
	private final ThirstSystem            system;
	@NonNull
	private final ThirstSynchronizer      synchronizer;
	@NonNull
	private final ThirstPenalties         penalties;
	@NonNull
	private final TemperatureMetabolism   metabolism;
	@NonNull
	private final BiomeEnvironmentSampler environment;
	@NonNull
	private final LoadExertion            exertion;

	public void tick (@NonNull ServerPlayer player) {
		((ThirstMovementAccess) player).sverve$setThirstGameplay(this);
		var state = players.update(player, current -> system.advance(current, player.isSprinting(), environment.humidity(player), metabolism.thirstHeat(player), SECONDS_PER_TICK));
		penalties.tick(player, state);
		synchronizer.update(player);
	}

	public void moved (@NonNull ServerPlayer player, double dx, double dy, double dz) {
		double loss = exertion.waterLoss(player, dx, dy, dz);
		if (loss == 0.0) return;
		players.update(player, state -> system.exert(state, loss));
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
		return WaterBottles.isPlainWater(stack);
	}
}
