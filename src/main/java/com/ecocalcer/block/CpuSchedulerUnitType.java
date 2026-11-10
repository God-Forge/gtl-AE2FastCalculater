package com.ecocalcer.block;

import appeng.block.crafting.ICraftingUnitType;
import appeng.block.crafting.CraftingUnitBlock;
import com.ecocalcer.registries.ModBlocks;
import net.minecraft.world.item.Item;

/**
 * Unit type for the CPU enhancement scheduler. It contributes no storage bytes and no
 * co-processors: its only purpose is to mark the CPU it is built into as managed by the
 * enhanced executor.
 */
public class CpuSchedulerUnitType implements ICraftingUnitType {
    @Override
    public long getStorageBytes() {
        return 0L;
    }

    @Override
    public int getAcceleratorThreads() {
        return 0;
    }

    @Override
    public Item getItemFromType() {
        return ModBlocks.CPU_SCHEDULER_ITEM.get();
    }
}
