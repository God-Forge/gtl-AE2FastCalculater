package com.ecocalcer.planner;

/**
 * Marker interface mixed into AE2's {@code CraftingCalculation}. When {@link #ecocalcer$markFast()}
 * has been called, the calculation skips the per-tick {@code TickHandler} budget and the
 * corresponding {@code registerCraftingSimulation} call.
 */
public interface FastCalculationMarker {
    void ecocalcer$markFast();

    boolean ecocalcer$isFast();
}
