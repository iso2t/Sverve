package com.iso2t.sverve.survival.exertion;

import com.iso2t.easyconfig.api.annotations.Comment;
import com.iso2t.easyconfig.api.value.wrappers.BooleanValue;
import com.iso2t.easyconfig.api.value.wrappers.DoubleValue;
import lombok.Getter;

@Getter
public final class LoadExertionConfig {
	@Comment("Extra thirst from voluntary movement with Heavy Inventories installed. HI owns weight-based hunger costs.")
	private final BooleanValue enabled = BooleanValue.of(true);

	@Comment("No added thirst at or below this fraction of effective carrying capacity.")
	private final DoubleValue freeLoadRatio = DoubleValue.of(0.5, 0.0, 0.95);

	@Comment("Upper bound on load used for exertion; extreme inventories cannot cause unbounded thirst loss.")
	private final DoubleValue maximumLoadRatio = DoubleValue.of(1.5, 1.0, 3.0);

	@Comment("Extra hydration lost per voluntarily travelled block at 100% carrying capacity.")
	private final DoubleValue waterLossPerBlock = DoubleValue.of(0.00025, 0.0, 0.01);

	@Comment("Multiplier on this added water loss while sprinting.")
	private final DoubleValue sprintMultiplier = DoubleValue.of(1.5, 1.0, 4.0);
}
