package com.ecocalcer.planner;

import appeng.api.networking.IGrid;
import appeng.me.service.CraftingService;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;

/**
 * Small reflection helpers used by the Mixin that replaces the crafting calculation.
 * <p>
 * AE2 obfuscates Minecraft member names (SRG), but not its own class/field names, so
 * {@code CraftingService#grid} and {@code CraftingService#CRAFTING_POOL} are stable.
 * Using reflection here avoids {@code @Shadow} mapping problems with AE2 classes that
 * are not part of the Minecraft mapping set.
 */
public final class CraftingServiceReflect {
    private static final Field GRID;
    private static final Field CRAFTING_POOL;

    static {
        try {
            GRID = CraftingService.class.getDeclaredField("grid");
            GRID.setAccessible(true);
            CRAFTING_POOL = CraftingService.class.getDeclaredField("CRAFTING_POOL");
            CRAFTING_POOL.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private CraftingServiceReflect() {
    }

    public static IGrid grid(CraftingService service) {
        try {
            return (IGrid) GRID.get(service);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to read CraftingService#grid", e);
        }
    }

    public static ExecutorService craftingPool() {
        try {
            return (ExecutorService) CRAFTING_POOL.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to read CraftingService#CRAFTING_POOL", e);
        }
    }
}
