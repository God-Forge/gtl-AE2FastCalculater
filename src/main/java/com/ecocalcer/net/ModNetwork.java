package com.ecocalcer.net;

import com.ecocalcer.EcoCalcMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EcoCalcMod.MOD_ID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private ModNetwork() {
    }

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, CycleGraphPacket.class,
                CycleGraphPacket::encode,
                CycleGraphPacket::decode,
                CycleGraphPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, CycleGraphPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
