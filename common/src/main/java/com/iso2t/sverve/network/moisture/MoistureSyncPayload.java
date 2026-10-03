package com.iso2t.sverve.network.moisture;

import com.iso2t.sverve.Constants;
import lombok.NonNull;
import lombok.Value;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Value
public class MoistureSyncPayload implements CustomPacketPayload {
	public static final Type<MoistureSyncPayload>                         TYPE  = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "moisture"));
	public static final StreamCodec<FriendlyByteBuf, MoistureSyncPayload> CODEC = CustomPacketPayload.codec((payload, buffer) -> {
		buffer.writeVarInt(payload.snapshot.getFillUnits());
		buffer.writeBoolean(payload.snapshot.isEnabled());
	}, buffer -> new MoistureSyncPayload(new MoistureSnapshot(buffer.readVarInt(), buffer.readBoolean())));

	@NonNull
	MoistureSnapshot snapshot;

	@Override
	public Type<MoistureSyncPayload> type () {
		return TYPE;
	}
}
