package com.iso2t.sverve.player.moisture;

public final class MoistureUpdateClock {

	private int elapsedTicks;

	public boolean advance () {
		if (++elapsedTicks < 20) return false;
		reset();
		return true;
	}

	public void reset () {
		elapsedTicks = 0;
	}
}
