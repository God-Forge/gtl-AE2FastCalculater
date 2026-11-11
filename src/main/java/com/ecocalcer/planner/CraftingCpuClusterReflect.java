package com.ecocalcer.planner;

import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.me.cluster.MBCalculator;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection helpers for the CPU combiner hooks.
 * <p>
 * {@code MBCalculator.target} is protected and {@code CraftingCPUCluster.addBlockEntity}
 * is package-private, so they cannot be referenced directly from our mixin package.
 */
public final class CraftingCpuClusterReflect {
    private static final Field TARGET = findField(MBCalculator.class, "target");
    private static final Method ADD_BLOCK_ENTITY = findMethod(
            CraftingCPUCluster.class, "addBlockEntity", CraftingBlockEntity.class);
    private static final Method DONE = findMethod(CraftingCPUCluster.class, "done");

    private CraftingCpuClusterReflect() {
    }

    private static Field findField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... params) {
        try {
            Method method = owner.getDeclaredMethod(name, params);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** Level of the block entity that started the multiblock calculation. */
    public static Level level(Object calculator) {
        try {
            BlockEntity target = (BlockEntity) TARGET.get(calculator);
            return target == null ? null : target.getLevel();
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static void addBlockEntity(CraftingCPUCluster cluster, CraftingBlockEntity blockEntity) {
        try {
            ADD_BLOCK_ENTITY.invoke(cluster, blockEntity);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to add block entity to CPU cluster", e);
        }
    }

    /** {@code CraftingCPUCluster#done()} is package-private. */
    public static void done(CraftingCPUCluster cluster) {
        try {
            DONE.invoke(cluster);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to finish CPU cluster", e);
        }
    }
}
