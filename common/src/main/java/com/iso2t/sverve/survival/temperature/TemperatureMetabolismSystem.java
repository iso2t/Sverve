package com.iso2t.sverve.survival.temperature;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Numeric consequences for body exposure; hunger and healing mechanics remain vanilla-owned.
 */
@RequiredArgsConstructor
public final class TemperatureMetabolismSystem {
	@NonNull
	private final TemperatureConfig config;

	public double thirstMultiplier (double exposure) {
		return switch (band(exposure)) {
			case WARM -> config.getWarmThirstMultiplier().get();
			case HOT -> config.getHotThirstMultiplier().get();
			default -> 1;
		};
	}

	public double foodExhaustionPerSecond (double exposure) {
		return switch (band(exposure)) {
			case WARM -> config.getWarmFoodExhaustion().get();
			case HOT -> config.getHotFoodExhaustion().get();
			default -> 0;
		};
	}

	public int healingInterval (int original, double exposure) {
		if (original < 1) throw new IllegalArgumentException("Healing interval must be positive");
		double multiplier = switch (band(exposure)) {
			case COLD -> config.getColdHealingIntervalMultiplier().get();
			case FREEZING -> config.getFreezingHealingIntervalMultiplier().get();
			default -> 1;
		};
		return (int) Math.min(Integer.MAX_VALUE, Math.ceil(original * multiplier));
	}

	private TemperatureBand band (double exposure) {
		var band = TemperatureBand.classify(exposure);
		return config.getEnabled().get() && config.getMetabolismEnabled().get() ? band : TemperatureBand.NORMAL;
	}
}
