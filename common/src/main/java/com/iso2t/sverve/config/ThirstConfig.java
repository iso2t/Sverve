package com.iso2t.sverve.config;

import com.iso2t.easyconfig.api.annotations.Comment;
import com.iso2t.easyconfig.api.value.wrappers.BooleanValue;
import com.iso2t.easyconfig.api.value.wrappers.DoubleValue;
import com.iso2t.easyconfig.api.value.wrappers.IntegerValue;
import lombok.Getter;

@Getter
public final class ThirstConfig {

	@Comment("Enable thirst drain, water bottle hydration, dehydration damage, and the thirst bar.")
	private final BooleanValue enabled = BooleanValue.of(true);

	@Comment("Base hydration lost per second. Hydration runs from 0 to 1.")
	private final DoubleValue baseLoss = DoubleValue.of(1.0 / 1200.0, 0.0, 1.0);

	@Comment("Additional hydration lost per second at maximum body heat, scaled by exposure while Warm/Hot. Requires temperature metabolism.")
	private final DoubleValue heatLoss = DoubleValue.of(0.0015, 0.0, 1.0);

	@Comment("Additional hydration lost per second while sprinting.")
	private final DoubleValue sprintLoss = DoubleValue.of(1.0 / 1200.0, 0.0, 1.0);

	@Comment("Hydration restored by finishing a plain water bottle. 0.3 restores 30% of the meter.")
	private final DoubleValue waterBottleHydration = DoubleValue.of(0.3, 0.0, 1.0);

	@Comment("Damage at zero hydration, in health points (2 = one heart). 0 pauses dehydration damage.")
	private final DoubleValue dehydrationDamage = DoubleValue.of(2.0, 0.0, 1000.0);

	@Comment("Seconds at zero hydration before each damage pulse. Drinking resets the timer.")
	private final IntegerValue dehydrationIntervalSeconds = IntegerValue.of(4, 1, 3600);

	@Comment("Additional hydration lost per second in completely dry air.")
	private final DoubleValue dryAirLoss = DoubleValue.of(0.0005, 0.0, 1.0);
}
