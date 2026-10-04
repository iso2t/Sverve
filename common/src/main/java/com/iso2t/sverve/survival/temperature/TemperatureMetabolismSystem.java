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

	public double thirstHeat (double exposure) {
		return thirstHeat(exposure, TemperatureProtectionScore.NONE);
	}

	public double thirstHeat (double exposure, @NonNull TemperatureProtectionScore protection) {
		return switch (band(exposure)) {
			case WARM, HOT -> protection.insulate(exposure);
			default -> 0;
		};
	}

	public double foodExhaustionPerSecond (double exposure) {
		return foodExhaustionPerSecond(exposure, TemperatureProtectionScore.NONE);
	}

	public double foodExhaustionPerSecond (double exposure, @NonNull TemperatureProtectionScore protection) {
		double exhaustion = switch (band(exposure)) {
			case WARM -> config.getWarmFoodExhaustion().get();
			case HOT -> config.getHotFoodExhaustion().get();
			default -> 0;
		};
		return exhaustion * (1 - protection.getHeatReduction());
	}

	public int healingInterval (int original, double exposure) {
		return healingInterval(original, exposure, TemperatureProtectionScore.NONE);
	}

	public int healingInterval (int original, double exposure, @NonNull TemperatureProtectionScore protection) {
		if (original < 1) throw new IllegalArgumentException("Healing interval must be positive");
		double multiplier = switch (band(exposure)) {
			case COLD -> config.getColdHealingIntervalMultiplier().get();
			case FREEZING -> config.getFreezingHealingIntervalMultiplier().get();
			default -> 1;
		};
		double protectedMultiplier = 1 + (multiplier - 1) * (1 - protection.getColdReduction());
		return (int) Math.min(Integer.MAX_VALUE, Math.ceil(original * protectedMultiplier));
	}

	private TemperatureBand band (double exposure) {
		var band = TemperatureBand.classify(exposure);
		return config.getEnabled().get() && config.getMetabolismEnabled().get() ? band : TemperatureBand.NORMAL;
	}
}
