package com.ecocalcer.planner;

import net.minecraft.core.BlockPos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry of crafting CPUs that are managed by a "CPU Scheduler" block.
 * <p>
 * The key is the lower corner of the CPU multiblock ({@code CraftingCPUCluster#getBoundsMin}).
 * The CPU enhancement hooks (recipe recycling, self-loop execution) only apply to CPUs
 * present in this set, which is what makes the scheduler block meaningful.
 */
public final class ManagedCpuRegistry {
    private static final Set<BlockPos> MANAGED = ConcurrentHashMap.newKeySet();

    private ManagedCpuRegistry() {
    }

    public static void setManaged(BlockPos boundsMin, boolean managed) {
        if (managed) {
            MANAGED.add(boundsMin);
        } else {
            MANAGED.remove(boundsMin);
        }
    }

    public static boolean isManaged(BlockPos boundsMin) {
        return MANAGED.contains(boundsMin);
    }

    public static void clear() {
        MANAGED.clear();
    }
}
