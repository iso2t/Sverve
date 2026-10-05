package com.iso2t.sverve.player.thirst;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@UtilityClass
public class FabricItemConsumptionEvents {

	public static final Event<Finished> FINISHED = EventFactory.createArrayBacked(Finished.class, listeners -> (player, consumed) -> {
		for (Finished listener : listeners) listener.onFinished(player, consumed);
	});

	@FunctionalInterface
	public interface Finished {
		void onFinished (ServerPlayer player, ItemStack consumed);
	}
}
