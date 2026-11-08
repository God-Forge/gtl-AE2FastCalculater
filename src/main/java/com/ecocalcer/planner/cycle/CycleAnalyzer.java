package com.ecocalcer.planner.cycle;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a bounded snapshot of the pattern dependency graph around a crafting target and
 * detects cycles (strongly connected components) in it.
 * <p>
 * The graph is intentionally bounded so that analysing a large network cannot stall the
 * game: at most {@link #MAX_NODES} keys and {@link #MAX_DEPTH} expansion levels are used.
 */
public final class CycleAnalyzer {
    public static final int MAX_NODES = 384;
    public static final int MAX_DEPTH = 8;

    private CycleAnalyzer() {
    }

    public static CycleReport analyze(IGrid grid, AEKey target, long amount) {
        long startNanos = System.nanoTime();
        ICraftingService crafting = grid.getCraftingService();

        // key -> node id
        Map<AEKey, Integer> ids = new LinkedHashMap<>();
        List<AEKey> keys = new ArrayList<>();
        Map<AEKey, Integer> depthOf = new HashMap<>();
        Set<Long> edgeSet = new HashSet<>();
        List<int[]> edges = new ArrayList<>();
        int truncated = 0;

        ids.put(target, 0);
        keys.add(target);
        depthOf.put(target, 0);

        ArrayDeque<AEKey> queue = new ArrayDeque<>();
        queue.add(target);

        while (!queue.isEmpty()) {
            AEKey key = queue.poll();
            int depth = depthOf.getOrDefault(key, 0);

            for (IPatternDetails pattern : crafting.getCraftingFor(key)) {
                GenericStack[] outputs = pattern.getOutputs();
                if (outputs.length == 0) {
                    continue;
                }
                for (IPatternDetails.IInput input : pattern.getInputs()) {
                    GenericStack[] possible = input.getPossibleInputs();
                    if (possible.length == 0) {
                        continue;
                    }
                    AEKey inputKey = possible[0].what();
                    if (inputKey == null) {
                        continue;
                    }

                    int from = ids.get(key);
                    Integer existing = ids.get(inputKey);
                    if (existing == null) {
                        if (keys.size() >= MAX_NODES) {
                            truncated++;
                            continue;
                        }
                        existing = keys.size();
                        ids.put(inputKey, existing);
                        keys.add(inputKey);
                    }
                    int to = existing;

                    long edgeId = ((long) from << 32) | (to & 0xFFFFFFFFL);
                    if (edgeSet.add(edgeId)) {
                        edges.add(new int[]{from, to});
                    }

                    if (depth + 1 <= MAX_DEPTH && !depthOf.containsKey(inputKey)) {
                        depthOf.put(inputKey, depth + 1);
                        queue.add(inputKey);
                    }
                }
            }
        }

        // Kosaraju SCC over the key graph.
        int n = keys.size();
        List<int[]> adjacency = new ArrayList<>(n);
        List<int[]> reverse = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adjacency.add(new int[0]);
            reverse.add(new int[0]);
        }
        int[] degree = new int[n];
        int[] rdegree = new int[n];
        for (int[] e : edges) {
            degree[e[0]]++;
            rdegree[e[1]]++;
        }
        int[][] adj = new int[n][];
        int[][] radj = new int[n][];
        for (int i = 0; i < n; i++) {
            adj[i] = new int[degree[i]];
            radj[i] = new int[rdegree[i]];
        }
        int[] adjIndex = new int[n];
        int[] radjIndex = new int[n];
        for (int[] e : edges) {
            adj[e[0]][adjIndex[e[0]]++] = e[1];
            radj[e[1]][radjIndex[e[1]]++] = e[0];
        }

        int[] order = new int[n];
        boolean[] visited = new boolean[n];
        int orderSize = 0;
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                orderSize = dfsOrder(adj, i, visited, order, orderSize);
            }
        }

        int[] sccId = new int[n];
        java.util.Arrays.fill(sccId, -1);
        int sccCount = 0;
        int[] stack = new int[n];
        int stackSize = 0;
        for (int idx = orderSize - 1; idx >= 0; idx--) {
            int v = order[idx];
            if (sccId[v] == -1) {
                stackSize = dfsAssign(radj, v, sccId, sccCount, stack, stackSize);
                sccCount++;
            }
        }

        // Group sizes.
        int[] sizes = new int[sccCount];
        for (int i = 0; i < n; i++) {
            sizes[sccId[i]]++;
        }

        // A component is cyclic if it has more than one node or a self-loop.
        boolean[] selfLoop = new boolean[sccCount];
        for (int[] e : edges) {
            if (e[0] == e[1]) {
                selfLoop[sccId[e[0]]] = true;
            }
        }
        boolean hasCycles = false;
        List<CycleReport.Group> groups = new ArrayList<>();
        for (int g = 0; g < sccCount; g++) {
            boolean cyclic = sizes[g] > 1 || selfLoop[g];
            hasCycles |= cyclic;
            if (cyclic) {
                groups.add(new CycleReport.Group(g, sizes[g], false));
            }
        }

        List<CycleReport.Node> nodes = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            nodes.add(new CycleReport.Node(keys.get(i), sccId[i], i == 0,
                    depthOf.getOrDefault(keys.get(i), 0)));
        }

        List<CycleReport.Edge> cycleEdges = new ArrayList<>(edges.size());
        for (int[] e : edges) {
            cycleEdges.add(new CycleReport.Edge(e[0], e[1]));
        }

        List<CycleReport.SeedDemand> seeds = hasCycles
                ? CycleSolver.solve(grid, target, amount)
                : List.of();

        long micros = (System.nanoTime() - startNanos) / 1000L;
        return new CycleReport(micros, hasCycles, target, amount, nodes, cycleEdges, groups, seeds, truncated);
    }

    private static int dfsOrder(int[][] adj, int start, boolean[] visited, int[] order, int orderSize) {
        // Iterative DFS to avoid deep recursion on large graphs.
        int[] nodeStack = new int[adj.length + 1];
        int[] indexStack = new int[adj.length + 1];
        int top = 0;
        nodeStack[top] = start;
        indexStack[top] = 0;
        visited[start] = true;
        top++;

        while (top > 0) {
            int v = nodeStack[top - 1];
            int idx = indexStack[top - 1];
            if (idx < adj[v].length) {
                indexStack[top - 1] = idx + 1;
                int w = adj[v][idx];
                if (!visited[w]) {
                    visited[w] = true;
                    nodeStack[top] = w;
                    indexStack[top] = 0;
                    top++;
                }
            } else {
                top--;
                order[orderSize++] = v;
            }
        }
        return orderSize;
    }

    private static int dfsAssign(int[][] radj, int start, int[] sccId, int id, int[] stack, int stackSize) {
        stack[stackSize++] = start;
        sccId[start] = id;
        while (stackSize > 0) {
            int v = stack[--stackSize];
            for (int w : radj[v]) {
                if (sccId[w] == -1) {
                    sccId[w] = id;
                    stack[stackSize++] = w;
                }
            }
        }
        return stackSize;
    }

    private static Set<AEKey> keysOf(List<AEKey> keys) {
        return new LinkedHashSet<>(keys);
    }
}
