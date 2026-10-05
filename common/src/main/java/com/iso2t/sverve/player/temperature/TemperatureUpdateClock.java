package com.iso2t.sverve.player.temperature;

public final class TemperatureUpdateClock {

	public static final int INTERVAL_TICKS = 20;
	private             int elapsedTicks;

	public boolean advance () {
		if (++elapsedTicks < INTERVAL_TICKS) return false;
		reset();
		return true;
	}

	public void reset () {
		elapsedTicks = 0;
	}
}
