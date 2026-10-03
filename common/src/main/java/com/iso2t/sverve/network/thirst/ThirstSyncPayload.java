package com.iso2t.sverve.network.thirst;

import com.iso2t.sverve.Constants;
import lombok.NonNull;
import lombok.Value;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Only sent from the server to the owning player.
 */
@Value
public class ThirstSyncPayload implements CustomPacketPayload {
	public static final Type<ThirstSyncPayload>                         TYPE  = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "thirst"));
	public static final StreamCodec<FriendlyByteBuf, ThirstSyncPayload> CODEC = CustomPacketPayload.codec((payload, buffer) -> {
		buffer.writeVarInt(payload.snapshot.getHalfUnits());
		buffer.writeBoolean(payload.snapshot.isEnabled());
	}, buffer -> new ThirstSyncPayload(new ThirstSnapshot(buffer.readVarInt(), buffer.readBoolean())));

	@NonNull
	ThirstSnapshot snapshot;

	@Override
	public Type<ThirstSyncPayload> type () {
		return TYPE;
	}
}
