package com.iso2t.sverve.config;

import com.iso2t.easyconfig.api.annotations.Comment;
import com.iso2t.easyconfig.api.value.wrappers.BooleanValue;
import com.iso2t.easyconfig.api.value.wrappers.DoubleValue;
import com.iso2t.easyconfig.api.value.wrappers.IntegerValue;
import lombok.Getter;

@Getter
public final class HeatSourceConfig {

	@Comment("Allow nearby lit campfires, placed torches, and lava to warm players and speed up drying.")
	private final BooleanValue enabled = BooleanValue.of(true);

	@Comment("Campfire warmth radius in blocks. Bounded to keep world sampling inexpensive.")
	private final IntegerValue campfireRadius = IntegerValue.of(4, 1, 8);

	@Comment("Maximum campfire warmth in normalized temperature units, fading to zero at its radius.")
	private final DoubleValue campfireWarmth = DoubleValue.of(0.8, 0.0, 1.0);

	@Comment("Torch warmth radius in blocks.")
	private final IntegerValue torchRadius = IntegerValue.of(2, 1, 8);

	@Comment("Maximum torch warmth in normalized temperature units, fading to zero at its radius.")
	private final DoubleValue torchWarmth = DoubleValue.of(0.2, 0.0, 1.0);

	@Comment("Lava warmth radius in blocks, including source and flowing lava.")
	private final IntegerValue lavaRadius = IntegerValue.of(6, 1, 8);

	@Comment("Maximum lava warmth in normalized temperature units, fading to zero at its radius.")
	private final DoubleValue lavaWarmth = DoubleValue.of(1.0, 0.0, 1.0);
}
