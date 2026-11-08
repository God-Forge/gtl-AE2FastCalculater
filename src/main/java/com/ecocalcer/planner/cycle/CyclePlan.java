package com.ecocalcer.planner.cycle;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;

import java.util.Map;

/**
 * A concrete crafting plan for a cyclic (self-replicating) recipe chain, produced by
 * {@link CycleSolver#solvePlan}.
 * <p>
 * Unlike {@link CycleReport} (which is only advisory), this carries the numbers that
 * AE2's crafting plan GUI needs: pattern execution counts, extracted items, emitted
 * items and missing items. It is turned into an {@code ICraftingPlan} (simulation mode)
 * so the confirmation screen can show the result instead of hanging forever.
 */
public record CyclePlan(
        AEKey target,
        long amount,
        boolean hasCycle,
        Map<IPatternDetails, Long> patternTimes,
        KeyCounter usedItems,
        KeyCounter emittedItems,
        KeyCounter missingItems,
        long bytes) {
}
