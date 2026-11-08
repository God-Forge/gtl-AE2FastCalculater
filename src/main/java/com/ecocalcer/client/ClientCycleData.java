package com.ecocalcer.client;

import com.ecocalcer.net.CycleGraphPacket;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side cache of the most recent cycle graph per planner block. */
public final class ClientCycleData {
    private static final Map<BlockPos, CycleGraphPacket> DATA = new ConcurrentHashMap<>();

    private ClientCycleData() {
    }

    public static void accept(CycleGraphPacket packet) {
        DATA.put(packet.pos(), packet);
    }

    public static CycleGraphPacket get(BlockPos pos) {
        return DATA.get(pos);
    }

    public static void clear() {
        DATA.clear();
    }
}
