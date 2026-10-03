package com.iso2t.sverve.network.temperature;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.survival.temperature.TemperatureBand;
import lombok.NonNull;
import lombok.Value;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Value
public class TemperatureSyncPayload implements CustomPacketPayload {
	public static final Type<TemperatureSyncPayload>                         TYPE  = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "temperature"));
	public static final StreamCodec<FriendlyByteBuf, TemperatureSyncPayload> CODEC = CustomPacketPayload.codec((payload, buffer) -> {
		buffer.writeVarInt(payload.snapshot.getBand().getId());
		buffer.writeBoolean(payload.snapshot.isEnabled());
	}, buffer -> new TemperatureSyncPayload(new TemperatureSnapshot(TemperatureBand.fromId(buffer.readVarInt()), buffer.readBoolean())));

	@NonNull
	TemperatureSnapshot snapshot;

	@Override
	public Type<TemperatureSyncPayload> type () {
		return TYPE;
	}
}
