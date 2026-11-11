package com.ecocalcer.block;

import appeng.block.crafting.ICraftingUnitType;
import appeng.block.crafting.CraftingUnitBlock;
import com.ecocalcer.registries.ModBlocks;
import net.minecraft.world.item.Item;

/**
 * Unit type for the CPU combiner. It contributes no storage and no co-processors; its
 * only purpose is to lift AE2's CPU shape restrictions for the CPU it is part of.
 */
public class CpuCombinerUnitType implements ICraftingUnitType {
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
        return ModBlocks.CPU_COMBINER_ITEM.get();
    }
}
