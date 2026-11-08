package com.ecocalcer.menu;

import com.ecocalcer.blockentity.FastPlannerBlockEntity;
import com.ecocalcer.net.CycleGraphPackets;
import com.ecocalcer.net.ModNetwork;
import com.ecocalcer.planner.cycle.CycleReport;
import com.ecocalcer.registries.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Menu for the fast planner block. It only carries the two feature toggles and a
 * little runtime statistics; there are no slots.
 */
public class FastPlannerMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_FAST_PLANNER = 0;
    public static final int BUTTON_TOGGLE_CYCLE_SOLVER = 1;

    public static final int DATA_FLAGS = 0;
    public static final int DATA_LAST_MICROS = 1;
    public static final int DATA_LAST_CYCLES = 2;
    public static final int DATA_LAST_USED_FAST_PATH = 3;
    public static final int DATA_COUNT = 4;

    private final FastPlannerBlockEntity blockEntity;
    private final ContainerData data;

    /** Server-side constructor. */
    public FastPlannerMenu(int containerId, Inventory inventory, FastPlannerBlockEntity blockEntity) {
        super(ModMenus.FAST_PLANNER.get(), containerId);
        this.blockEntity = blockEntity;

        // On the server the values are read from the block entity and broadcast through the
        // data slots. On the client the synced values are written into a small cache, so the
        // screen shows live state even before the block entity data packet arrives.
        boolean clientSide = inventory.player.level().isClientSide();
        this.data = new ContainerData() {
            private final int[] synced = new int[DATA_COUNT];

            @Override
            public int get(int index) {
                return clientSide ? synced[index] : blockEntity.getMenuData(index);
            }

            @Override
            public void set(int index, int value) {
                synced[index] = value;
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        addDataSlots(this.data);

        if (inventory.player instanceof ServerPlayer serverPlayer) {
            CycleReport report = blockEntity.getLastReport();
            if (report != null) {
                ModNetwork.sendToPlayer(serverPlayer,
                        CycleGraphPackets.from(blockEntity.getBlockPos(), report));
            }
        }
    }

    /** Client-side constructor. */
    public FastPlannerMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, findBlockEntity(inventory, buf.readBlockPos()));
    }

    private static FastPlannerBlockEntity findBlockEntity(Inventory inventory, net.minecraft.core.BlockPos pos) {
        Level level = inventory.player.level();
        if (level.getBlockEntity(pos) instanceof FastPlannerBlockEntity blockEntity) {
            return blockEntity;
        }
        throw new IllegalStateException("Fast planner block entity missing at " + pos);
    }

    public FastPlannerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getMenuData(int index) {
        return data.get(index);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (blockEntity.isRemoved()) {
            return false;
        }
        switch (buttonId) {
            case BUTTON_TOGGLE_FAST_PLANNER -> blockEntity.toggleFlag(FastPlannerBlockEntity.FLAG_FAST_PLANNER);
            case BUTTON_TOGGLE_CYCLE_SOLVER -> blockEntity.toggleFlag(FastPlannerBlockEntity.FLAG_CYCLE_SOLVER);
            default -> {
                return false;
            }
        }
        return true;
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

    public boolean isFastPlannerEnabled() {
        return (data.get(DATA_FLAGS) & FastPlannerBlockEntity.FLAG_FAST_PLANNER) != 0;
    }

    public boolean isCycleSolverEnabled() {
        return (data.get(DATA_FLAGS) & FastPlannerBlockEntity.FLAG_CYCLE_SOLVER) != 0;
    }
}
