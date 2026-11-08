package com.ecocalcer.net;

import appeng.api.stacks.AEKey;
import com.ecocalcer.client.ClientCycleData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server-to-client snapshot of the cyclic dependency graph of the last crafting plan
 * calculation. The screen renders this as a small graph.
 */
public record CycleGraphPacket(
        BlockPos pos,
        long analysisMicros,
        boolean hasCycles,
        AEKey target,
        long targetAmount,
        List<NodeEntry> nodes,
        List<EdgeEntry> edges,
        List<SeedEntry> seeds) {

    public record NodeEntry(AEKey key, int sccId, boolean target) {
    }

    public record EdgeEntry(int from, int to) {
    }

    public record SeedEntry(AEKey key, long amount) {
    }

    public static void encode(CycleGraphPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarLong(packet.analysisMicros);
        buf.writeBoolean(packet.hasCycles);
        AEKey.writeKey(buf, packet.target);
        buf.writeVarLong(packet.targetAmount);

        buf.writeVarInt(packet.nodes.size());
        for (NodeEntry node : packet.nodes) {
            AEKey.writeKey(buf, node.key);
            buf.writeVarInt(node.sccId);
            buf.writeBoolean(node.target);
        }

        buf.writeVarInt(packet.edges.size());
        for (EdgeEntry edge : packet.edges) {
            buf.writeVarInt(edge.from);
            buf.writeVarInt(edge.to);
        }

        buf.writeVarInt(packet.seeds.size());
        for (SeedEntry seed : packet.seeds) {
            AEKey.writeKey(buf, seed.key);
            buf.writeVarLong(seed.amount);
        }
    }

    public static CycleGraphPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        long micros = buf.readVarLong();
        boolean hasCycles = buf.readBoolean();
        AEKey target = AEKey.readKey(buf);
        long targetAmount = buf.readVarLong();

        int nodeCount = buf.readVarInt();
        List<NodeEntry> nodes = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            AEKey key = AEKey.readKey(buf);
            int sccId = buf.readVarInt();
            boolean isTarget = buf.readBoolean();
            nodes.add(new NodeEntry(key, sccId, isTarget));
        }

        int edgeCount = buf.readVarInt();
        List<EdgeEntry> edges = new ArrayList<>(edgeCount);
        for (int i = 0; i < edgeCount; i++) {
            edges.add(new EdgeEntry(buf.readVarInt(), buf.readVarInt()));
        }

        int seedCount = buf.readVarInt();
        List<SeedEntry> seeds = new ArrayList<>(seedCount);
        for (int i = 0; i < seedCount; i++) {
            AEKey key = AEKey.readKey(buf);
            long amount = buf.readVarLong();
            seeds.add(new SeedEntry(key, amount));
        }

        return new CycleGraphPacket(pos, micros, hasCycles, target, targetAmount, nodes, edges, seeds);
    }

    public static void handle(CycleGraphPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> ClientCycleData.accept(packet));
        context.setPacketHandled(true);
    }
}
