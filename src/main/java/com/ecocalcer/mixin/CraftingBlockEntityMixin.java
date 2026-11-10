package com.ecocalcer.mixin;

import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.ecocalcer.blockentity.CpuSchedulerBlockEntity;
import com.ecocalcer.planner.ManagedCpuRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks the CPU that contains a {@link CpuSchedulerBlockEntity} as managed.
 * <p>
 * {@code CraftingBlockEntity#updateStatus(CraftingCPUCluster)} is called for every block
 * of a formed CPU multiblock, with the freshly created cluster. When the block is our
 * scheduler unit, the cluster's lower corner is registered in
 * {@link ManagedCpuRegistry}, which enables the enhanced executor for exactly that CPU.
 * <p>
 * Targeting AE2's own method names is not remapped, hence {@code remap = false}.
 */
@Mixin(CraftingBlockEntity.class)
public abstract class CraftingBlockEntityMixin {
    @Inject(method = "updateStatus", at = @At("HEAD"), remap = false)
    private void ecocalcer$markSchedulerCpu(CraftingCPUCluster cluster, CallbackInfo ci) {
        if ((Object) this instanceof CpuSchedulerBlockEntity && cluster != null) {
            ManagedCpuRegistry.setManaged(cluster.getBoundsMin(), true);
        }
    }
}
