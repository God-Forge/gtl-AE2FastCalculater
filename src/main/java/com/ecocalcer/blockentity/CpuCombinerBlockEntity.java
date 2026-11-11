package com.ecocalcer.blockentity;

import appeng.blockentity.crafting.CraftingBlockEntity;
import com.ecocalcer.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity of the CPU combiner. A normal AE2 crafting unit entity; the shape-rule
 * bypass is applied by {@code CraftingCpuCalculatorMixin} while the CPU is being formed.
 */
public class CpuCombinerBlockEntity extends CraftingBlockEntity {
    public CpuCombinerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public CpuCombinerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CPU_COMBINER.get(), pos, state);
    }
}
