package com.iso2t.sverve.player.thirst;

import lombok.Getter;

/**
 * Transient, server-tick time at zero hydration. Never persisted or advanced while paused.
 */
@Getter
public final class DehydrationTimer {
	private int elapsedTicks;

	public boolean advance (int intervalTicks) {
		if (intervalTicks < 1) throw new IllegalArgumentException("Damage interval must be positive");
		if (++elapsedTicks < intervalTicks) return false;
		reset();
		return true;
	}

	public void reset () {
		elapsedTicks = 0;
	}
}
