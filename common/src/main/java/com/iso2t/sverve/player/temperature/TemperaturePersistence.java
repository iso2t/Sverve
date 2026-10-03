package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.survival.temperature.TemperatureState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.experimental.UtilityClass;

/**
 * Versioned temperature save format shared by both native attachment implementations.
 */
@UtilityClass
public class TemperaturePersistence {
	private static final int           VERSION  = 1;
	private static final Codec<Double> EXPOSURE = Codec.DOUBLE.validate(value -> Double.isFinite(value) && value >= -1.0 && value <= 1.0 ? DataResult.success(value) : DataResult.error(() -> "Temperature exposure must be finite and between -1 and 1"));

	public static final MapCodec<TemperatureState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(Codec.intRange(VERSION, VERSION).fieldOf("version").forGetter(state -> VERSION), EXPOSURE.fieldOf("exposure").forGetter(TemperatureState::getExposure)).apply(instance, (version, exposure) -> new TemperatureState(exposure)));
}
