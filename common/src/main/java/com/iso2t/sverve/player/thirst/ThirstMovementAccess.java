package com.iso2t.sverve.player.thirst;

/**
 * Server-player mixin bridge; gameplay is rebound on native ticks after player replacement.
 */
public interface ThirstMovementAccess {
	void sverve$setThirstGameplay (ThirstGameplay gameplay);
}
