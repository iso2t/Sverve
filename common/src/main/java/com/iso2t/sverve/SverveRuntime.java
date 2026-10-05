package com.iso2t.sverve;

import com.iso2t.sverve.config.SurvivalConfig;
import com.iso2t.sverve.player.moisture.MoistureFeature;
import com.iso2t.sverve.player.temperature.TemperatureFeature;
import com.iso2t.sverve.player.thirst.ThirstFeature;
import lombok.NonNull;

public record SverveRuntime(@NonNull SurvivalConfig config, @NonNull ThirstFeature thirst, @NonNull TemperatureFeature temperature, @NonNull MoistureFeature moisture) {
}
