package com.iso2t.sverve.network.temperature;

import com.iso2t.sverve.survival.temperature.TemperatureBand;
import lombok.NonNull;

public record TemperatureSnapshot(@NonNull TemperatureBand band, boolean enabled) {
}
