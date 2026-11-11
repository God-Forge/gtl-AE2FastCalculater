package com.ecocalcer.registries;

import com.ecocalcer.EcoCalcMod;
import com.ecocalcer.block.CpuCombinerBlock;
import com.ecocalcer.block.CpuSchedulerBlock;
import com.ecocalcer.block.FastPlannerBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;

public final class ModBlocks {
    private ModBlocks() {
    }

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EcoCalcMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EcoCalcMod.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EcoCalcMod.MOD_ID);

    public static final RegistryObject<FastPlannerBlock> FAST_PLANNER =
            BLOCKS.register("fast_planner", FastPlannerBlock::new);

    public static final RegistryObject<BlockItem> FAST_PLANNER_ITEM =
            ITEMS.register("fast_planner", () -> new BlockItem(FAST_PLANNER.get(), new Item.Properties()));

    public static final RegistryObject<CpuSchedulerBlock> CPU_SCHEDULER =
            BLOCKS.register("cpu_scheduler", CpuSchedulerBlock::new);

    public static final RegistryObject<BlockItem> CPU_SCHEDULER_ITEM =
            ITEMS.register("cpu_scheduler", () -> new BlockItem(CPU_SCHEDULER.get(), new Item.Properties()));

    public static final RegistryObject<CpuCombinerBlock> CPU_COMBINER =
            BLOCKS.register("cpu_combiner", CpuCombinerBlock::new);

    public static final RegistryObject<BlockItem> CPU_COMBINER_ITEM =
            ITEMS.register("cpu_combiner", () -> new BlockItem(CPU_COMBINER.get(), new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.ecocalcer"))
            .icon(() -> new ItemStack(FAST_PLANNER_ITEM.get()))
            .displayItems((parameters, output) -> {
                output.accept(FAST_PLANNER_ITEM.get());
                output.accept(CPU_SCHEDULER_ITEM.get());
                output.accept(CPU_COMBINER_ITEM.get());
            })
            .build());
}
