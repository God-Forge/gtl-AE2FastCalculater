package com.ecocalcer.menu;

import com.ecocalcer.blockentity.CpuSchedulerBlockEntity;
import com.ecocalcer.net.CpuListPacket;
import com.ecocalcer.net.ModNetwork;
import com.ecocalcer.registries.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Menu for the CPU scheduler block. It has no slots; the CPU list is synchronized through
 * {@link CpuListPacket} and toggled through {@code CpuTogglePacket}.
 */
public class CpuSchedulerMenu extends AbstractContainerMenu {
    private final CpuSchedulerBlockEntity blockEntity;
    private List<CpuListPacket.Entry> clientCpus = List.of();

    public CpuSchedulerMenu(int containerId, Inventory inventory, CpuSchedulerBlockEntity blockEntity) {
        super(ModMenus.CPU_SCHEDULER.get(), containerId);
        this.blockEntity = blockEntity;
        if (inventory.player instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendToPlayer(serverPlayer, CpuListPacket.from(blockEntity));
        }
    }

    public CpuSchedulerMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, findBlockEntity(inventory, buf.readBlockPos()));
    }

    private static CpuSchedulerBlockEntity findBlockEntity(Inventory inventory, net.minecraft.core.BlockPos pos) {
        Level level = inventory.player.level();
        if (level.getBlockEntity(pos) instanceof CpuSchedulerBlockEntity blockEntity) {
            return blockEntity;
        }
        throw new IllegalStateException("CPU scheduler block entity missing at " + pos);
    }

    public CpuSchedulerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public List<CpuListPacket.Entry> getClientCpus() {
        return clientCpus;
    }

    public void setClientCpus(List<CpuListPacket.Entry> clientCpus) {
        this.clientCpus = clientCpus;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null
                && !blockEntity.isRemoved()
                && player.level() == blockEntity.getLevel()
                && player.distanceToSqr(
                        blockEntity.getBlockPos().getX() + 0.5,
                        blockEntity.getBlockPos().getY() + 0.5,
                        blockEntity.getBlockPos().getZ() + 0.5) < 64.0
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
