package com.iso2t.sverve.player.moisture;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.survival.moisture.MoistureState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class FabricMoistureStorage implements MoistureStorage {
	private final AttachmentType<MoistureState>       moisture = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "moisture"), builder -> builder.initializer(MoistureState::dry).persistent(MoisturePersistence.CODEC.codec()));
	// No copyOnDeath: death respawn starts dry regardless of keepInventory.
	private final AttachmentType<MoistureUpdateClock> clock    = AttachmentRegistry.create(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "moisture_clock"), builder -> builder.initializer(MoistureUpdateClock::new));

	@Override
	public MoistureState get (ServerPlayer player) {
		return player.getAttachedOrCreate(moisture);
	}

	@Override
	public void set (ServerPlayer player, MoistureState state) {
		player.setAttached(moisture, state);
	}

	@Override
	public MoistureUpdateClock clock (ServerPlayer player) {
		return player.getAttachedOrCreate(clock);
	}
}
