package com.goro.network;

import com.goro.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoyé du serveur → client pour ouvrir le GUI avec la config actuelle. */
public class OpenGuiPacket {

    private final String configJson;

    public OpenGuiPacket(String configJson) {
        this.configJson = configJson;
    }

    public static void encode(OpenGuiPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.configJson, 1 << 20); // max 1 Mo
    }

    public static OpenGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenGuiPacket(buf.readUtf(1 << 20));
    }

    public static void handle(OpenGuiPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientPacketHandler.openGui(pkt.configJson)
            )
        );
        ctx.get().setPacketHandled(true);
    }
}
