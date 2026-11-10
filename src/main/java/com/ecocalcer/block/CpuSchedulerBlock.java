package com.ecocalcer.block;

import appeng.block.crafting.CraftingUnitBlock;

/**
 * The "CPU Scheduler" block, built into an AE2 crafting CPU multiblock. A CPU that
 * contains this block is driven by the enhanced executor (see
 * {@link com.ecocalcer.planner.ManagedCpuRegistry} and {@code CraftingBlockEntityMixin}).
 * <p>
 * It behaves like a normal crafting unit with no storage and no co-processors, so it
 * only changes the CPU's executor, never its capacity.
 */
public class CpuSchedulerBlock extends CraftingUnitBlock {
    public CpuSchedulerBlock() {
        super(new CpuSchedulerUnitType());
    }
}
