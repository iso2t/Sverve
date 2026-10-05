package com.iso2t.sverve.player.thirst;

import com.iso2t.sverve.survival.thirst.ThirstState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ThirstPersistence {

	private static final int           VERSION   = 1;
	private static final Codec<Double> HYDRATION = Codec.DOUBLE.validate(value -> Double.isFinite(value) && value >= 0.0 && value <= 1.0 ? DataResult.success(value) : DataResult.error(() -> "Hydration must be finite and between 0 and 1"));

	public static final MapCodec<ThirstState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(Codec.intRange(VERSION, VERSION).fieldOf("version").forGetter(_ -> VERSION), HYDRATION.fieldOf("hydration").forGetter(ThirstState::hydration)).apply(instance, (_, hydration) -> new ThirstState(hydration)));
}
