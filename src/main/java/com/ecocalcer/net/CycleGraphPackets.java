package com.ecocalcer.net;

import com.ecocalcer.planner.cycle.CycleReport;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Converts a server-side {@link CycleReport} into the packet sent to the client. */
public final class CycleGraphPackets {
    private CycleGraphPackets() {
    }

    public static CycleGraphPacket from(BlockPos pos, CycleReport report) {
        List<CycleGraphPacket.NodeEntry> nodes = new ArrayList<>(report.nodes().size());
        for (CycleReport.Node node : report.nodes()) {
            nodes.add(new CycleGraphPacket.NodeEntry(node.key(), node.sccId(), node.target()));
        }
        List<CycleGraphPacket.EdgeEntry> edges = new ArrayList<>(report.edges().size());
        for (CycleReport.Edge edge : report.edges()) {
            edges.add(new CycleGraphPacket.EdgeEntry(edge.from(), edge.to()));
        }
        List<CycleGraphPacket.SeedEntry> seeds = new ArrayList<>(report.seeds().size());
        for (CycleReport.SeedDemand seed : report.seeds()) {
            seeds.add(new CycleGraphPacket.SeedEntry(seed.key(), seed.amount()));
        }
        return new CycleGraphPacket(pos, report.analysisMicros(), report.hasCycles(),
                report.target(), report.targetAmount(), nodes, edges, seeds);
    }
}
