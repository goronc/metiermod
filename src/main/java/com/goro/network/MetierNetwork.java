package com.goro.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class MetierNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("metiermod", "main"),
        () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, OpenGuiPacket.class,
            OpenGuiPacket::encode, OpenGuiPacket::decode, OpenGuiPacket::handle);
        CHANNEL.registerMessage(id++, SaveConfigPacket.class,
            SaveConfigPacket::encode, SaveConfigPacket::decode, SaveConfigPacket::handle);
        CHANNEL.registerMessage(id++, SyncCapabilityPacket.class,
            SyncCapabilityPacket::encode, SyncCapabilityPacket::decode, SyncCapabilityPacket::handle);
    }
}
