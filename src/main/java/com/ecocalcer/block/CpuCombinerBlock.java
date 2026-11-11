package com.ecocalcer.block;

import appeng.block.crafting.CraftingUnitBlock;

/**
 * The "CPU Combiner" block. A CPU whose multiblock contains this block ignores AE2's
 * vanilla shape rules: size limits (16 blocks per axis) are lifted and holes inside the
 * bounding box are tolerated.
 */
public class CpuCombinerBlock extends CraftingUnitBlock {
    public CpuCombinerBlock() {
        super(new CpuCombinerUnitType());
    }
}
