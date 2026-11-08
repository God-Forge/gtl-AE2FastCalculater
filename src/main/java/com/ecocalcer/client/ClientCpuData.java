package com.ecocalcer.client;

import com.ecocalcer.net.CpuListPacket;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side cache of the CPU list sent by a scheduler block. */
public final class ClientCpuData {
    private static final Map<BlockPos, List<CpuListPacket.Entry>> DATA = new ConcurrentHashMap<>();

    private ClientCpuData() {
    }

    public static void accept(CpuListPacket packet) {
        DATA.put(packet.schedulerPos(), packet.cpus());
    }

    public static List<CpuListPacket.Entry> get(BlockPos schedulerPos) {
        return DATA.getOrDefault(schedulerPos, List.of());
    }

    public static void clear() {
        DATA.clear();
    }
}
