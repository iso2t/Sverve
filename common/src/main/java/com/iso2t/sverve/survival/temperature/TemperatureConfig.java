package com.iso2t.sverve.survival.temperature;

import com.iso2t.easyconfig.api.annotations.Comment;
import com.iso2t.easyconfig.api.value.wrappers.BooleanValue;
import com.iso2t.easyconfig.api.value.wrappers.DoubleValue;
import com.iso2t.easyconfig.api.value.wrappers.IntegerValue;
import lombok.Getter;

@Getter
public final class TemperatureConfig {
	@Comment("Enable body temperature changes, penalties, and its HUD icon.")
	private final BooleanValue enabled = BooleanValue.of(true);

	@Comment("Biome base temperature considered comfortable. Vanilla plains use 0.8; these are not Celsius units.")
	private final DoubleValue comfortableBiomeTemperature = DoubleValue.of(0.8, -10.0, 10.0);

	@Comment("Biome temperature drop below comfortable that maps to full cold exposure. Must be positive.")
	private final DoubleValue coldBiomeRange = DoubleValue.of(0.8, 0.01, 100.0);

	@Comment("Biome temperature rise above comfortable that maps to full heat exposure. Must be positive.")
	private final DoubleValue hotBiomeRange = DoubleValue.of(1.2, 0.01, 100.0);

	@Comment("Response rate per second toward the effective ambient temperature.")
	private final DoubleValue responseRate = DoubleValue.of(0.02, 0.0, 1.0);

	@Comment("Cooling at full wetness, in normalized temperature units.")
	private final DoubleValue wetCooling = DoubleValue.of(0.35, 0.0, 1.0);

	@Comment("Enable heat-driven hunger/thirst loss and cold-delayed natural healing.")
	private final BooleanValue metabolismEnabled = BooleanValue.of(true);

	@Comment("Additional vanilla food exhaustion per second while Warm. Saturation is spent before hunger.")
	private final DoubleValue warmFoodExhaustion = DoubleValue.of(0.1, 0.0, 10.0);

	@Comment("Additional vanilla food exhaustion per second while Hot. Saturation is spent before hunger.")
	private final DoubleValue hotFoodExhaustion = DoubleValue.of(0.2, 0.0, 10.0);

	@Comment("Multiplier on vanilla natural healing intervals while Cold. Does not affect healing effects or potions.")
	private final DoubleValue coldHealingIntervalMultiplier = DoubleValue.of(1.5, 1.0, 20.0);

	@Comment("Multiplier on vanilla natural healing intervals while Freezing. Does not affect healing effects or potions.")
	private final DoubleValue freezingHealingIntervalMultiplier = DoubleValue.of(2.0, 1.0, 20.0);

	@Comment("Health points lost per freezing pulse before temperature enchantment protection. Zero disables cold damage.")
	private final DoubleValue freezingDamage = DoubleValue.of(2.0, 0.0, 1000.0);

	@Comment("Health points lost per overheating pulse before temperature enchantment protection. Zero disables heat damage.")
	private final DoubleValue overheatingDamage = DoubleValue.of(2.0, 0.0, 1000.0);

	@Comment("Seconds of continuous unprotected extreme temperature between damage pulses.")
	private final IntegerValue damageIntervalSeconds = IntegerValue.of(4, 1, 3600);
}
