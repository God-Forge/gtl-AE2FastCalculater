package com.ecocalcer.blockentity;

import appeng.api.networking.GridFlags;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.api.util.AECableType;
import com.ecocalcer.planner.cycle.CycleReport;
import com.ecocalcer.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * The in-network planner block. It currently only stores the two feature toggles
 * ("fast planner" and "cycle solver") plus a little runtime statistics and
 * exposes them to the client.
 */
public class FastPlannerBlockEntity extends AENetworkBlockEntity {
    public static final int FLAG_FAST_PLANNER = 1;
    public static final int FLAG_CYCLE_SOLVER = 1 << 1;

    private static final String NBT_FLAGS = "flags";

    private int flags;

    private long lastPlanMicros;
    private int lastPlanCycles;
    private int lastPlanUsedFastPath;
    @Nullable
    private CycleReport lastReport;

    public FastPlannerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // This block never belongs to a channel-consuming machine: it must keep working
        // even when the network has no free channel, otherwise planning could get stuck.
        getMainNode().setIdlePowerUsage(0.0D);
    }

    /**
     * Constructor used by {@link net.minecraft.world.level.block.entity.BlockEntityType.Builder}.
     */
    public FastPlannerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.FAST_PLANNER.get(), pos, state);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        this.flags = data.getInt(NBT_FLAGS);
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putInt(NBT_FLAGS, this.flags);
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    public int getFlags() {
        return flags;
    }

    public boolean hasFlag(int flag) {
        return (flags & flag) != 0;
    }

    public void setFlag(int flag, boolean enabled) {
        int next = enabled ? (flags | flag) : (flags & ~flag);
        if (next != flags) {
            flags = next;
            setChanged();
            markForUpdate();
        }
    }

    public void toggleFlag(int flag) {
        setFlag(flag, !hasFlag(flag));
    }

    // ---------------------------------------------------------------------
    // Client synchronization through the open Menu (ContainerData).
    // ---------------------------------------------------------------------

    public int getMenuData(int index) {
        return switch (index) {
            case 0 -> flags;
            case 1 -> (int) Math.min(Integer.MAX_VALUE, lastPlanMicros);
            case 2 -> lastPlanCycles;
            case 3 -> lastPlanUsedFastPath;
            default -> 0;
        };
    }

    public void recordPlan(long micros, int cycles, boolean usedFastPath) {
        this.lastPlanMicros = micros;
        this.lastPlanCycles = cycles;
        this.lastPlanUsedFastPath = usedFastPath ? 1 : 0;
    }

    public long getLastPlanMicros() {
        return lastPlanMicros;
    }

    public int getLastPlanCycles() {
        return lastPlanCycles;
    }

    @Nullable
    public CycleReport getLastReport() {
        return lastReport;
    }

    public void setLastReport(@Nullable CycleReport report) {
        this.lastReport = report;
    }
}
