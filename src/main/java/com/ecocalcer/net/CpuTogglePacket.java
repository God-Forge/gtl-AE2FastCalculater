package com.ecocalcer.net;

import com.ecocalcer.blockentity.CpuSchedulerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client-to-server: toggle a crafting CPU in the scheduler block. */
public record CpuTogglePacket(BlockPos schedulerPos, BlockPos boundsMin) {
    public static void encode(CpuTogglePacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.schedulerPos);
        buf.writeBlockPos(packet.boundsMin);
    }

    public static CpuTogglePacket decode(FriendlyByteBuf buf) {
        return new CpuTogglePacket(buf.readBlockPos(), buf.readBlockPos());
    }

    public static void handle(CpuTogglePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (player.level().getBlockEntity(packet.schedulerPos) instanceof CpuSchedulerBlockEntity scheduler) {
                scheduler.toggleCpu(packet.boundsMin);
                ModNetwork.sendToPlayer(player, CpuListPacket.from(scheduler));
            }
        });
        context.setPacketHandled(true);
    }
}
