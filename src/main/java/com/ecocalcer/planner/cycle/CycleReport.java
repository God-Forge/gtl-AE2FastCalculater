package com.ecocalcer.planner.cycle;

import appeng.api.stacks.AEKey;

import java.util.List;

/**
 * A snapshot of the crafting dependency graph around one crafting target, produced by
 * {@link CycleAnalyzer}. It is immutable and small enough to be sent to the client.
 */
public record CycleReport(
        long analysisMicros,
        boolean hasCycles,
        AEKey target,
        long targetAmount,
        List<Node> nodes,
        List<Edge> edges,
        List<Group> groups,
        List<SeedDemand> seeds,
        int truncated) {

    /** A single resource key in the dependency graph. */
    public record Node(AEKey key, int sccId, boolean target, int depth) {
    }

    /** A directed dependency edge: one pattern consumes {@code to} to produce {@code from}. */
    public record Edge(int from, int to) {
    }

    /** A strongly connected component. */
    public record Group(int sccId, int size, boolean selfGrowing) {
    }

    /** A resource the cycle solver could not get from the network or from cyclic crafting. */
    public record SeedDemand(AEKey key, long amount) {
    }

    public static CycleReport empty(AEKey target, long amount) {
        return new CycleReport(0, false, target, amount, List.of(), List.of(), List.of(), List.of(), 0);
    }
}
