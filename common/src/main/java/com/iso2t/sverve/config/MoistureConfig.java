package com.iso2t.sverve.config;

import com.iso2t.easyconfig.api.annotations.Comment;
import com.iso2t.easyconfig.api.value.wrappers.BooleanValue;
import com.iso2t.easyconfig.api.value.wrappers.DoubleValue;
import lombok.Getter;

@Getter
public final class MoistureConfig {

	@Comment("Enable changes to wetness and its cooling contribution.")
	private final BooleanValue enabled = BooleanValue.of(true);

	@Comment("Wetness gained per second while exposed to rain.")
	private final DoubleValue rainGain = DoubleValue.of(0.05, 0.0, 1.0);

	@Comment("Wetness lost per second in dry air, before heat adds extra drying.")
	private final DoubleValue dryingRate = DoubleValue.of(0.01, 0.0, 1.0);

	@Comment("Additional drying per second at maximum ambient heat. Humidity slows drying.")
	private final DoubleValue heatDryingRate = DoubleValue.of(0.02, 0.0, 1.0);
}
