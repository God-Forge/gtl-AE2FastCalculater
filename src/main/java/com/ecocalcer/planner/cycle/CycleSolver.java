package com.ecocalcer.planner.cycle;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A bounded, best-effort solver for cyclic recipe dependencies.
 * <p>
 * It walks the crafting tree from the requested output, consumes the network inventory
 * virtually and keeps descending through patterns. Unlike AE2's recursive planner this
 * walk is breadth-first, bounded in depth and steps, and it tracks which resource is
 * needed in a cycle without having a "seed" in stock. That information is what the GUI
 * presents as the "cycle graph" and the required seed materials.
 * <p>
 * The result is advisory: the actual crafting plan is still produced by AE2's planner
 * (accelerated by the fast planner switch).
 */
public final class CycleSolver {
    private static final int MAX_STEPS = 20000;
    private static final int MAX_DEPTH = 96;
    private static final long MAX_REPORTED = 1_000_000_000_000L;

    private CycleSolver() {
    }

    public static List<CycleReport.SeedDemand> solve(IGrid grid, AEKey target, long amount) {
        ICraftingService crafting = grid.getCraftingService();
        KeyCounter inventory = grid.getStorageService().getCachedInventory();

        Map<AEKey, Long> reserved = new HashMap<>();
        Map<AEKey, Long> missing = new LinkedHashMap<>();
        ArrayDeque<Demand> queue = new ArrayDeque<>();
        queue.add(new Demand(target, amount, 0));

        int steps = 0;
        while (!queue.isEmpty() && steps < MAX_STEPS) {
            Demand demand = queue.poll();
            steps++;

            long need = demand.amount;
            if (need <= 0) {
                continue;
            }

            // Use whatever the network already has.
            long available = inventory.get(demand.key) - reserved.getOrDefault(demand.key, 0L);
            if (available > 0) {
                long used = Math.min(available, need);
                reserved.merge(demand.key, used, Long::sum);
                need -= used;
            }
            if (need <= 0) {
                continue;
            }

            if (demand.depth >= MAX_DEPTH) {
                merge(missing, demand.key, need);
                continue;
            }

            IPatternDetails pattern = pickPattern(crafting, demand.key);
            if (pattern == null) {
                merge(missing, demand.key, need);
                continue;
            }

            long perCraft = outputAmount(pattern, demand.key);
            if (perCraft <= 0) {
                merge(missing, demand.key, need);
                continue;
            }

            long times = Math.min(need, Long.MAX_VALUE / 2);
            times = (times + perCraft - 1) / perCraft;

            for (IPatternDetails.IInput input : pattern.getInputs()) {
                GenericStack[] possible = input.getPossibleInputs();
                if (possible.length == 0 || possible[0].what() == null) {
                    continue;
                }
                long inputAmount;
                try {
                    inputAmount = Math.multiplyExact(Math.multiplyExact(input.getMultiplier(), times),
                            possible[0].amount());
                } catch (ArithmeticException overflow) {
                    inputAmount = Long.MAX_VALUE;
                }
                queue.add(new Demand(possible[0].what(), inputAmount, demand.depth + 1));
            }
        }

        List<CycleReport.SeedDemand> seeds = new ArrayList<>();
        for (Map.Entry<AEKey, Long> entry : missing.entrySet()) {
            seeds.add(new CycleReport.SeedDemand(entry.getKey(), Math.min(entry.getValue(), MAX_REPORTED)));
        }
        return seeds;
    }

    private static IPatternDetails pickPattern(ICraftingService crafting, AEKey key) {
        for (IPatternDetails pattern : crafting.getCraftingFor(key)) {
            if (outputAmount(pattern, key) > 0) {
                return pattern;
            }
        }
        return null;
    }

    private static long outputAmount(IPatternDetails pattern, AEKey key) {
        long total = 0;
        for (GenericStack output : pattern.getOutputs()) {
            if (key.equals(output.what())) {
                total += output.amount();
            }
        }
        return total;
    }

    private static void merge(Map<AEKey, Long> map, AEKey key, long amount) {
        long current = map.getOrDefault(key, 0L);
        long next = current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
        map.put(key, next);
    }

    /**
     * Solves a concrete *preview* plan for a self-replicating cycle.
     * <p>
     * Example: a smithing template duplication pattern consumes one template (plus
     * material) and produces two templates. Starting from one template in the network
     * the cycle grows by one template per craft, so {@code N - stock} crafts are needed.
     * AE2's own planner refuses this pattern because its input equals its output, so the
     * plan can never be produced without a dedicated solver like this one.
     * <p>
     * The returned plan is a simulation ({@code simulation = true}): it is shown by the
     * confirmation screen, but it cannot be dispatched, because AE2's CPU task model
     * requires all inputs of a task up-front while a self-replicating pattern feeds
     * itself from its own output.
     */
    public static CyclePlan solvePlan(IGrid grid, AEKey target, long amount) {
        ICraftingService crafting = grid.getCraftingService();
        KeyCounter inventory = grid.getStorageService().getCachedInventory();

        KeyCounter used = new KeyCounter();
        KeyCounter emitted = new KeyCounter();
        KeyCounter missing = new KeyCounter();
        Map<IPatternDetails, Long> patternTimes = new LinkedHashMap<>();
        Map<AEKey, Long> consumed = new HashMap<>();

        ArrayDeque<Demand> queue = new ArrayDeque<>();
        queue.add(new Demand(target, amount, 0));
        boolean hasCycle = false;
        int steps = 0;

        while (!queue.isEmpty() && steps < MAX_STEPS) {
            Demand demand = queue.poll();
            steps++;

            long need = demand.amount;
            if (need <= 0) {
                continue;
            }

            // Use whatever the network already has.
            long available = inventory.get(demand.key) - consumed.getOrDefault(demand.key, 0L);
            if (available > 0) {
                long use = Math.min(available, need);
                consumed.merge(demand.key, use, Long::sum);
                used.add(demand.key, use);
                need -= use;
            }
            if (need <= 0) {
                continue;
            }

            if (demand.depth >= MAX_DEPTH) {
                missing.add(demand.key, need);
                continue;
            }

            IPatternDetails pattern = pickPattern(crafting, demand.key);
            if (pattern == null) {
                missing.add(demand.key, need);
                continue;
            }

            long out = outputAmount(pattern, demand.key);
            if (out <= 0) {
                missing.add(demand.key, need);
                continue;
            }

            long selfIn = selfInputAmount(pattern, demand.key);
            if (selfIn > 0) {
                hasCycle = true;
                long per = out - selfIn;
                if (per <= 0) {
                    missing.add(demand.key, need);
                    continue;
                }
                long times = (need + per - 1) / per;
                patternTimes.merge(pattern, times, Long::sum);

                // The cycle needs at least selfIn copies of the key in the loop as a seed.
                long stockUsed = consumed.getOrDefault(demand.key, 0L);
                long seedMissing = Math.max(0L, selfIn - stockUsed);
                if (seedMissing > 0) {
                    missing.add(demand.key, seedMissing);
                }

                for (IPatternDetails.IInput input : pattern.getInputs()) {
                    GenericStack[] possible = input.getPossibleInputs();
                    if (possible.length == 0 || possible[0].what() == null) {
                        continue;
                    }
                    AEKey inKey = possible[0].what();
                    if (inKey.equals(demand.key)) {
                        continue;
                    }
                    long inAmount = safeMultiply(input.getMultiplier(), times, possible[0].amount());
                    queue.add(new Demand(inKey, inAmount, demand.depth + 1));
                }
            } else {
                long times = (need + out - 1) / out;
                patternTimes.merge(pattern, times, Long::sum);

                for (IPatternDetails.IInput input : pattern.getInputs()) {
                    GenericStack[] possible = input.getPossibleInputs();
                    if (possible.length == 0 || possible[0].what() == null) {
                        continue;
                    }
                    long inAmount = safeMultiply(input.getMultiplier(), times, possible[0].amount());
                    queue.add(new Demand(possible[0].what(), inAmount, demand.depth + 1));
                }
            }
        }

        long bytes = 0;
        for (long t : patternTimes.values()) {
            bytes += t * 8L;
        }
        return new CyclePlan(target, amount, hasCycle, patternTimes, used, emitted, missing, bytes);
    }

    private static long selfInputAmount(IPatternDetails pattern, AEKey key) {
        long total = 0;
        for (IPatternDetails.IInput input : pattern.getInputs()) {
            GenericStack[] possible = input.getPossibleInputs();
            if (possible.length == 0 || possible[0].what() == null) {
                continue;
            }
            if (key.equals(possible[0].what())) {
                total += input.getMultiplier() * possible[0].amount();
            }
        }
        return total;
    }

    private static long safeMultiply(long a, long b, long c) {
        try {
            return Math.multiplyExact(Math.multiplyExact(a, b), c);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    private record Demand(AEKey key, long amount, int depth) {
    }
}
