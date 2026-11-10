package com.ecocalcer.blockentity;

import appeng.blockentity.crafting.CraftingBlockEntity;
import com.ecocalcer.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity of the CPU scheduler. It is a normal AE2 crafting unit block entity; the
 * "managed" marker is applied by {@code CraftingBlockEntityMixin} when the surrounding
 * CPU multiblock is formed.
 */
public class CpuSchedulerBlockEntity extends CraftingBlockEntity {
    public CpuSchedulerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public CpuSchedulerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CPU_SCHEDULER.get(), pos, state);
    }
}
