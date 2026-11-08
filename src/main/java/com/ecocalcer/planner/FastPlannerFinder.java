package com.ecocalcer.planner;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import com.ecocalcer.blockentity.FastPlannerBlockEntity;

import java.util.Set;

/**
 * Locates the enabled planner block on an AE2 grid. The block has to be active
 * on the network: if it is unloaded or disconnected, the vanilla planner is used.
 */
public final class FastPlannerFinder {
    private FastPlannerFinder() {
    }

    public static FastPlannerBlockEntity find(IGrid grid) {
        Set<FastPlannerBlockEntity> machines = grid.getMachines(FastPlannerBlockEntity.class);
        for (FastPlannerBlockEntity machine : machines) {
            if (machine.getMainNode().isActive()) {
                return machine;
            }
        }
        // Fall back to a manual scan in case the machine cache is stale.
        for (IGridNode node : grid.getNodes()) {
            if (node.getOwner() instanceof FastPlannerBlockEntity machine && machine.getMainNode().isActive()) {
                return machine;
            }
        }
        return null;
    }

    public static FastPlannerOptions optionsOf(FastPlannerBlockEntity machine) {
        return new FastPlannerOptions(
                machine.hasFlag(FastPlannerBlockEntity.FLAG_FAST_PLANNER),
                machine.hasFlag(FastPlannerBlockEntity.FLAG_CYCLE_SOLVER));
    }
}
