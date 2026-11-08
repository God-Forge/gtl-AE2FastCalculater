package com.ecocalcer.registries;

import com.ecocalcer.EcoCalcMod;
import com.ecocalcer.menu.FastPlannerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private ModMenus() {
    }

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, EcoCalcMod.MOD_ID);

    public static final RegistryObject<MenuType<FastPlannerMenu>> FAST_PLANNER =
            MENUS.register("fast_planner", () -> IForgeMenuType.create(FastPlannerMenu::new));
}
