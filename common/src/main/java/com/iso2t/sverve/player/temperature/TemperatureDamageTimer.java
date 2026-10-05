package com.iso2t.sverve.player.temperature;

import com.iso2t.sverve.survival.temperature.TemperatureBand;
import lombok.Getter;
import lombok.NonNull;

@Getter
public final class TemperatureDamageTimer {

	private TemperatureBand band;
	private int             elapsedTicks;

	public boolean advance (@NonNull TemperatureBand nextBand, int intervalTicks) {
		if (nextBand != TemperatureBand.FREEZING && nextBand != TemperatureBand.HOT) {
			throw new IllegalArgumentException("Only extreme temperature advances the damage timer");
		}
		if (intervalTicks < 1) throw new IllegalArgumentException("Damage interval must be positive");
		if (band != nextBand) {
			reset();
			band = nextBand;
		}
		if (++elapsedTicks < intervalTicks) return false;
		elapsedTicks = 0;
		return true;
	}

	public void reset () {
		band = null;
		elapsedTicks = 0;
	}
}
