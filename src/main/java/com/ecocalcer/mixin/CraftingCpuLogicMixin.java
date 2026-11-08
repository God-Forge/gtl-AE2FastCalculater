package com.ecocalcer.mixin;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.crafting.execution.CraftingCpuLogic;
import com.ecocalcer.EcoCalcMod;
import com.ecocalcer.planner.CraftingCpuReflect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * CPU executor hook: recycling for self-replicating cycles.
 * <p>
 * AE2's CPU outputs pattern results directly; a self-replicating pattern (input == output,
 * e.g. smithing template duplication) therefore starves after its seed is consumed.
 * When the CPU is managed by a {@code CPU Scheduler} block and the current job contains
 * such a pattern, one unit of each produced stack is put back into the CPU's internal
 * inventory, so the next execution of the pattern can consume it. The remaining amount
 * follows AE2's normal insert path (network output).
 * <p>
 * This only applies to {@link com.ecocalcer.planner.ManagedCpuRegistry} CPUs; all other
 * CPUs keep vanilla behaviour.
 */
@Mixin(CraftingCpuLogic.class)
public abstract class CraftingCpuLogicMixin {
    @ModifyVariable(method = "insert", at = @At("HEAD"), argsOnly = true, index = 2, remap = false)
    private long ecocalcer$recycleSelfLoop(long amount, AEKey what, Actionable type) {
        if (amount <= 0) {
            return amount;
        }
        try {
            if (CraftingCpuReflect.shouldRecycle(this, what)) {
                CraftingCpuReflect.recycle(this, what);
                return amount - 1;
            }
        } catch (Throwable t) {
            EcoCalcMod.LOGGER.warn("CPU recycle failed", t);
        }
        return amount;
    }
}
