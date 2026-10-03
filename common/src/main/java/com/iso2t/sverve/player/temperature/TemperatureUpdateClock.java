package com.iso2t.sverve.player.temperature;

/**
 * Transient eligible game ticks between one-second samples. No world or wall-clock time.
 */
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
