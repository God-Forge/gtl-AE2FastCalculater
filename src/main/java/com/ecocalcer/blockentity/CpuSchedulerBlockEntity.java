package com.ecocalcer.blockentity;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.ecocalcer.planner.ManagedCpuRegistry;
import com.ecocalcer.registries.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The "CPU Scheduler" block. It keeps a persistent set of crafting CPUs (identified by the
 * lower corner of their multiblock) that should be driven by the enhanced executor.
 */
public class CpuSchedulerBlockEntity extends AENetworkBlockEntity {
    private static final String NBT_MANAGED = "managed";

    private final Set<BlockPos> managedCpus = new HashSet<>();

    public CpuSchedulerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        getMainNode().setIdlePowerUsage(0.0D);
    }

    public CpuSchedulerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CPU_SCHEDULER.get(), pos, state);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        managedCpus.clear();
        for (long packed : data.getLongArray(NBT_MANAGED)) {
            managedCpus.add(BlockPos.of(packed));
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putLongArray(NBT_MANAGED, managedCpus.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Override
    public void onReady() {
        super.onReady();
        syncRegistry();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        // Unregister on unload; onReady re-registers after a reload.
        for (BlockPos pos : managedCpus) {
            ManagedCpuRegistry.setManaged(pos, false);
        }
    }

    private void syncRegistry() {
        for (BlockPos pos : managedCpus) {
            ManagedCpuRegistry.setManaged(pos, true);
        }
    }

    public boolean isManaged(BlockPos boundsMin) {
        return managedCpus.contains(boundsMin);
    }

    public void toggleCpu(BlockPos boundsMin) {
        if (managedCpus.remove(boundsMin)) {
            ManagedCpuRegistry.setManaged(boundsMin, false);
        } else {
            managedCpus.add(boundsMin);
            ManagedCpuRegistry.setManaged(boundsMin, true);
        }
        setChanged();
    }

    public Set<BlockPos> getManagedCpus() {
        return managedCpus;
    }

    /** All crafting CPUs currently in this network, sorted by position. */
    public List<CraftingCPUCluster> getCpus() {
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return List.of();
        }
        List<CraftingCPUCluster> result = new ArrayList<>();
        for (ICraftingCPU cpu : grid.getCraftingService().getCpus()) {
            if (cpu instanceof CraftingCPUCluster cluster) {
                result.add(cluster);
            }
        }
        result.sort(Comparator.comparingLong(c -> c.getBoundsMin().asLong()));
        return result;
    }
}
