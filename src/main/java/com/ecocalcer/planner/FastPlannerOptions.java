package com.ecocalcer.planner;

/**
 * The runtime configuration read from the planner block GUI.
 *
 * @param fastPlanner  when enabled, AE2 crafting plan calculations run without the
 *                     per-tick 5 microsecond budget used by AE2's {@code TickHandler}
 * @param cycleSolver  when enabled, cyclic recipe dependencies are analysed and solved
 *                     before the (fast) plan calculation
 */
public record FastPlannerOptions(boolean fastPlanner, boolean cycleSolver) {
    public boolean isAnythingEnabled() {
        return fastPlanner || cycleSolver;
    }
}
