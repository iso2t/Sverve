package com.iso2t.sverve.network.thirst;

import com.iso2t.sverve.Constants;
import lombok.NonNull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ThirstSyncPayload(@NonNull ThirstSnapshot snapshot) implements CustomPacketPayload {

	public static final Type<ThirstSyncPayload>                         TYPE  = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "thirst"));
	public static final StreamCodec<FriendlyByteBuf, ThirstSyncPayload> CODEC = CustomPacketPayload.codec((payload, buffer) -> {
		buffer.writeVarInt(payload.snapshot.halfUnits());
		buffer.writeBoolean(payload.snapshot.enabled());
	}, buffer -> new ThirstSyncPayload(new ThirstSnapshot(buffer.readVarInt(), buffer.readBoolean())));

	@Override
	public @NonNull Type<ThirstSyncPayload> type () {
		return TYPE;
	}
}
