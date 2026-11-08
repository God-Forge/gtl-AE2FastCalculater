package com.ecocalcer.planner;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.me.cluster.implementations.CraftingCPUCluster;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Reflection helpers for the CPU executor hooks. AE2's {@code CraftingCpuLogic} and
 * {@code ExecutingCraftingJob} members are not part of the Minecraft mapping set, so
 * {@code @Shadow} cannot be used reliably; their names are stable in AE2's own jar.
 */
public final class CraftingCpuReflect {
    private static final Field CLUSTER = find(CraftingCpuLogic.class, "cluster");
    private static final Field JOB = find(CraftingCpuLogic.class, "job");
    private static final Field TASKS = find(ExecutingCraftingJob.class, "tasks");

    private CraftingCpuReflect() {
    }

    private static Field find(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static CraftingCPUCluster cluster(Object logic) {
        try {
            return (CraftingCPUCluster) CLUSTER.get(logic);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ExecutingCraftingJob job(Object logic) {
        try {
            return (ExecutingCraftingJob) JOB.get(logic);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<IPatternDetails, ?> tasks(Object job) {
        try {
            return (Map<IPatternDetails, ?>) TASKS.get(job);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * True when the given CPU is scheduler-managed and one of its pending tasks is a
     * self-replicating pattern for this key (pattern input == pattern output).
     */
    public static boolean shouldRecycle(Object logic, AEKey key) {
        CraftingCPUCluster cluster = cluster(logic);
        if (cluster == null || !ManagedCpuRegistry.isManaged(cluster.getBoundsMin())) {
            return false;
        }
        ExecutingCraftingJob job = job(logic);
        if (job == null) {
            return false;
        }
        for (IPatternDetails pattern : tasks(job).keySet()) {
            if (isSelfLoop(pattern, key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSelfLoop(IPatternDetails pattern, AEKey key) {
        boolean input = false;
        boolean output = false;
        for (IPatternDetails.IInput i : pattern.getInputs()) {
            GenericStack[] possible = i.getPossibleInputs();
            if (possible.length > 0 && key.equals(possible[0].what())) {
                input = true;
            }
        }
        for (GenericStack stack : pattern.getOutputs()) {
            if (key.equals(stack.what())) {
                output = true;
            }
        }
        return input && output;
    }

    /** Puts one unit of the key back into the CPU inventory so the cycle can feed itself. */
    public static void recycle(Object logic, AEKey key) {
        ((CraftingCpuLogic) logic).getInventory().insert(key, 1, Actionable.MODULATE);
    }
}
