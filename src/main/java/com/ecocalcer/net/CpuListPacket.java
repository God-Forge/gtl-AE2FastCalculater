package com.ecocalcer.net;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.ecocalcer.blockentity.CpuSchedulerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server-to-client list of crafting CPUs for the scheduler screen. */
public record CpuListPacket(BlockPos schedulerPos, List<Entry> cpus) {
    public record Entry(BlockPos boundsMin, String name, boolean busy, boolean managed, int coProcessors) {
    }

    public static void encode(CpuListPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.schedulerPos);
        buf.writeVarInt(packet.cpus.size());
        for (Entry entry : packet.cpus) {
            buf.writeBlockPos(entry.boundsMin());
            buf.writeUtf(entry.name(), 64);
            buf.writeBoolean(entry.busy());
            buf.writeBoolean(entry.managed());
            buf.writeVarInt(entry.coProcessors());
        }
    }

    public static CpuListPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readBlockPos(), buf.readUtf(64),
                    buf.readBoolean(), buf.readBoolean(), buf.readVarInt()));
        }
        return new CpuListPacket(pos, entries);
    }

    public static void handle(CpuListPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> com.ecocalcer.client.ClientCpuData.accept(packet));
        context.setPacketHandled(true);
    }

    public static CpuListPacket from(CpuSchedulerBlockEntity scheduler) {
        List<Entry> entries = new ArrayList<>();
        for (CraftingCPUCluster cpu : scheduler.getCpus()) {
            entries.add(new Entry(cpu.getBoundsMin(), cpu.getName().getString(),
                    cpu.isBusy(), scheduler.isManaged(cpu.getBoundsMin()), cpu.getCoProcessors()));
        }
        return new CpuListPacket(scheduler.getBlockPos(), entries);
    }
}
