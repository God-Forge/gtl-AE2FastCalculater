package com.ecocalcer;

import com.ecocalcer.blockentity.CpuSchedulerBlockEntity;
import com.ecocalcer.blockentity.FastPlannerBlockEntity;
import com.ecocalcer.net.ModNetwork;
import com.ecocalcer.registries.ModBlockEntities;
import com.ecocalcer.registries.ModBlocks;
import com.ecocalcer.registries.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EcoCalcMod.MOD_ID)
public class EcoCalcMod {
    public static final String MOD_ID = "ecocalcer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EcoCalcMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.ITEMS.register(modBus);
        ModBlocks.TABS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);

        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        ModNetwork.init();

        LOGGER.info("ECO Fast Planner initialized.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModBlocks.FAST_PLANNER.get().setBlockEntity(
                    FastPlannerBlockEntity.class,
                    ModBlockEntities.FAST_PLANNER.get(),
                    null,
                    null);
            ModBlocks.CPU_SCHEDULER.get().setBlockEntity(
                    appeng.blockentity.crafting.CraftingBlockEntity.class,
                    ModBlockEntities.CPU_SCHEDULER.get(),
                    null,
                    null);
            ModBlocks.CPU_COMBINER.get().setBlockEntity(
                    appeng.blockentity.crafting.CraftingBlockEntity.class,
                    ModBlockEntities.CPU_COMBINER.get(),
                    null,
                    null);
        });
    }
}
