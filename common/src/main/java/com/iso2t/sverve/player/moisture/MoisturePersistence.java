package com.iso2t.sverve.player.moisture;

import com.iso2t.sverve.survival.moisture.MoistureState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MoisturePersistence {
	private static final int           VERSION = 1;
	private static final Codec<Double> WETNESS = Codec.DOUBLE.validate(value -> Double.isFinite(value) && value >= 0 && value <= 1 ? DataResult.success(value) : DataResult.error(() -> "Wetness must be finite and between 0 and 1"));

	public static final MapCodec<MoistureState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(Codec.intRange(VERSION, VERSION).fieldOf("version").forGetter(state -> VERSION), WETNESS.fieldOf("wetness").forGetter(MoistureState::getWetness)).apply(instance, (version, wetness) -> new MoistureState(wetness)));
}
