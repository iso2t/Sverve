package com.iso2t.sverve.survival;

import com.iso2t.sverve.config.SurvivalConfig;
import com.iso2t.sverve.survival.environment.EnvironmentSample;
import com.iso2t.sverve.survival.moisture.MoistureSystem;
import com.iso2t.sverve.survival.temperature.TemperatureSystem;
import com.iso2t.sverve.survival.thirst.ThirstSystem;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates feature order and passes numeric contributions between independent systems.
 */
@RequiredArgsConstructor
public final class SurvivalEngine {
	private static final double TICKS_PER_SECOND = 20.0;

	@NonNull
	private final MoistureSystem    moisture;
	@NonNull
	private final TemperatureSystem temperature;
	@NonNull
	private final ThirstSystem      thirst;

	public static SurvivalEngine create (@NonNull SurvivalConfig config) {
		return new SurvivalEngine(new MoistureSystem(config.getMoisture()), new TemperatureSystem(config.getTemperature()), new ThirstSystem(config.getThirst()));
	}

	/**
	 * Simulates elapsed game ticks; the caller owns player eligibility and scheduling.
	 */
	public SurvivalState advance (@NonNull SurvivalState state, @NonNull EnvironmentSample environment, int elapsedTicks) {
		if (elapsedTicks < 0) throw new IllegalArgumentException("elapsedTicks must be nonnegative");
		if (elapsedTicks == 0) return state;

		double seconds = elapsedTicks / TICKS_PER_SECOND;
		var nextMoisture = moisture.advance(state.getMoisture(), environment, seconds);
		var nextTemperature = temperature.advance(state.getTemperature(), environment, moisture.coolingWetness(nextMoisture), seconds);
		var nextThirst = thirst.advance(state.getThirst(), environment, temperature.thirstHeat(nextTemperature), seconds);
		return new SurvivalState(nextTemperature, nextMoisture, nextThirst);
	}
}
