package com.goro.network;

import com.goro.config.MetierLevelConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoyé du client → serveur pour sauvegarder la config modifiée dans le GUI. */
public class SaveConfigPacket {

    private final String configJson;

    public SaveConfigPacket(String configJson) {
        this.configJson = configJson;
    }

    public static void encode(SaveConfigPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.configJson, 1 << 20);
    }

    public static SaveConfigPacket decode(FriendlyByteBuf buf) {
        return new SaveConfigPacket(buf.readUtf(1 << 20));
    }

    public static void handle(SaveConfigPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                MetierLevelConfig.fromJson(pkt.configJson);
                MetierLevelConfig.save();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
