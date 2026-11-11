package com.ecocalcer.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.GridCraftingCpuChange;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.ecocalcer.EcoCalcMod;
import com.ecocalcer.planner.CpuCombinerHelper;
import com.ecocalcer.planner.CraftingCpuClusterReflect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;

/**
 * Lets a CPU that contains a {@code CPU Combiner} block bypass AE2's vanilla shape rules:
 * <ul>
 *     <li>the 16-block-per-axis size limit is lifted;</li>
 *     <li>present blocks inside the bounding box no longer have to be a solid grid of
 *     crafting units (holes are tolerated);</li>
 *     <li>the CPU is formed from the crafting units that actually exist.</li>
 * </ul>
 * CPUs without a combiner keep vanilla behaviour.
 */
@Mixin(CraftingCPUCalculator.class)
public abstract class CraftingCpuCalculatorMixin {
    @Inject(method = "checkMultiblockScale", at = @At("HEAD"), cancellable = true, remap = false)
    private void ecocalcer$skipScale(BlockPos min, BlockPos max, CallbackInfoReturnable<Boolean> cir) {
        try {
            Level level = CraftingCpuClusterReflect.level(this);
            if (level != null && CpuCombinerHelper.hasCombiner(level, min, max)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable t) {
            EcoCalcMod.LOGGER.warn("CPU combiner scale check failed", t);
        }
    }

    @Inject(method = "verifyInternalStructure", at = @At("HEAD"), cancellable = true, remap = false)
    private void ecocalcer$skipStructure(ServerLevel level, BlockPos min, BlockPos max,
                                         CallbackInfoReturnable<Boolean> cir) {
        try {
            if (CpuCombinerHelper.hasCombiner(level, min, max)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable t) {
            EcoCalcMod.LOGGER.warn("CPU combiner structure check failed", t);
        }
    }

    @Inject(method = "updateBlockEntities", at = @At("HEAD"), cancellable = true, remap = false)
    private void ecocalcer$updateWithHoles(CraftingCPUCluster cluster, ServerLevel level,
                                           BlockPos min, BlockPos max, CallbackInfo ci) {
        try {
            if (!CpuCombinerHelper.hasCombiner(level, min, max)) {
                return;
            }

            // Vanilla assumes every position in [min, max] is a crafting unit and
            // hard-casts; with a combiner, skip non-unit positions (holes).
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                if (level.getBlockEntity(pos) instanceof CraftingBlockEntity blockEntity) {
                    blockEntity.updateStatus(cluster);
                    CraftingCpuClusterReflect.addBlockEntity(cluster, blockEntity);
                }
            }
            CraftingCpuClusterReflect.done(cluster);

            Iterator<CraftingBlockEntity> iterator = cluster.getBlockEntities();
            while (iterator.hasNext()) {
                CraftingBlockEntity blockEntity = iterator.next();
                IGridNode node = blockEntity.getGridNode();
                if (node == null) {
                    continue;
                }
                IGrid grid = node.getGrid();
                if (grid != null) {
                    grid.postEvent(new GridCraftingCpuChange(node));
                }
                break;
            }
            ci.cancel();
        } catch (Throwable t) {
            EcoCalcMod.LOGGER.warn("CPU combiner update failed", t);
        }
    }
}
