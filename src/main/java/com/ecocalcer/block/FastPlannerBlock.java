package com.ecocalcer.block;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.block.AEBaseBlock;
import appeng.block.AEBaseEntityBlock;
import com.ecocalcer.blockentity.FastPlannerBlockEntity;
import com.ecocalcer.menu.FastPlannerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * The in-network "fast planner" block. Right-click opens its configuration screen.
 */
public class FastPlannerBlock extends AEBaseEntityBlock<FastPlannerBlockEntity> {
    public FastPlannerBlock() {
        super(AEBaseBlock.metalProps());
    }

    @Override
    public InteractionResult onActivated(Level level, BlockPos pos, Player player, InteractionHand hand,
                                         @Nullable ItemStack heldItem, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        FastPlannerBlockEntity blockEntity = getBlockEntity(level, pos);
        if (blockEntity != null && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (containerId, inventory, p) -> new FastPlannerMenu(containerId, inventory, blockEntity),
                    Component.translatable("gui.ecocalcer.fast_planner")), pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.none();
    }
}
