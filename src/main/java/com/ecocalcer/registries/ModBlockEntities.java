package com.ecocalcer.registries;

import com.ecocalcer.EcoCalcMod;
import com.ecocalcer.blockentity.CpuSchedulerBlockEntity;
import com.ecocalcer.blockentity.FastPlannerBlockEntity;
import appeng.blockentity.crafting.CraftingBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private ModBlockEntities() {
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EcoCalcMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<FastPlannerBlockEntity>> FAST_PLANNER =
            BLOCK_ENTITIES.register("fast_planner", () -> BlockEntityType.Builder
                    .of(FastPlannerBlockEntity::new, ModBlocks.FAST_PLANNER.get())
                    .build(null));

    // The CPU scheduler is an AE2 crafting unit: its block/entity generic type is
    // CraftingBlockEntity, so the registry entry is widened accordingly.
    @SuppressWarnings("unchecked")
    public static final RegistryObject<BlockEntityType<CraftingBlockEntity>> CPU_SCHEDULER =
            BLOCK_ENTITIES.register("cpu_scheduler", () -> (BlockEntityType<CraftingBlockEntity>) (BlockEntityType<?>) BlockEntityType.Builder
                    .of(CpuSchedulerBlockEntity::new, ModBlocks.CPU_SCHEDULER.get())
                    .build(null));
}
