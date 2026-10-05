package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.item.WaterBottles;
import com.iso2t.sverve.network.thirst.ThirstSynchronizer;
import com.iso2t.sverve.player.environment.EnvironmentSampler;
import com.iso2t.sverve.player.exertion.MovementThirst;
import com.iso2t.sverve.player.temperature.TemperatureMetabolism;
import com.iso2t.sverve.survival.thirst.ThirstState;
import com.iso2t.sverve.survival.thirst.ThirstSystem;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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
	@NonNull
	private final EnvironmentSampler    environment;
	@NonNull
	private final MovementThirst        movement;

	public void tick (@NonNull ServerPlayer player) {
		((ThirstMovementAccess) player).sverve$bindThirst(this);
		var state = players.update(player, current -> drain(player, current));
		penalties.tick(player, state);
		synchronizer.update(player);
	}

	private ThirstState drain (ServerPlayer player, ThirstState state) {
		var humidity = environment.humidity(player);
		var heat = metabolism.thirstHeat(player);
		return system.advance(state, player.isSprinting(), humidity, heat, SECONDS_PER_TICK);
	}

	public void moved (@NonNull ServerPlayer player, double dx, double dy, double dz) {
		var loss = movement.waterLoss(player, dx, dy, dz);
		if (loss == 0.0) return;
		players.update(player, state -> system.exert(state, loss));
		synchronizer.update(player);
	}

	public void consume (@NonNull ServerPlayer player, @NonNull ItemStack consumed) {
		if (!WaterBottles.isPlainWater(consumed)) return;
		var state = players.update(player, system::drinkWater);
		penalties.hydrationChanged(player, state);
		synchronizer.update(player);
	}
}
