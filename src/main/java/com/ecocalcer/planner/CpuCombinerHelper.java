package com.ecocalcer.planner;

import com.ecocalcer.blockentity.CpuCombinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Helper to detect whether a CPU multiblock region contains a CPU combiner block.
 */
public final class CpuCombinerHelper {
    /** Upper bound on the number of positions scanned, to keep the check cheap. */
    private static final int MAX_SCANNED = 20000;

    private CpuCombinerHelper() {
    }

    public static boolean hasCombiner(Level level, BlockPos min, BlockPos max) {
        if (level == null) {
            return false;
        }
        long volume = ((long) max.getX() - min.getX() + 1)
                * ((long) max.getY() - min.getY() + 1)
                * ((long) max.getZ() - min.getZ() + 1);
        if (volume <= 0 || volume > MAX_SCANNED) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockEntity(pos) instanceof CpuCombinerBlockEntity) {
                return true;
            }
        }
        return false;
    }
}
