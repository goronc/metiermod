package com.goro.network;

import com.goro.capability.PlayerMetierProvider;
import com.goro.data.MetierPrincipal;
import com.goro.data.MetierSecondaire;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoyé du serveur → client pour synchroniser la capability métier du joueur local. */
public class SyncCapabilityPacket {

    private final MetierPrincipal  principal;
    private final int              principalLevel;
    private final MetierSecondaire secondaire;
    private final int              secondaireLevel;
    private final String           maitrise;
    private final int              maitriseLevel;

    public SyncCapabilityPacket(MetierPrincipal principal, int principalLevel,
                                MetierSecondaire secondaire, int secondaireLevel,
                                String maitrise, int maitriseLevel) {
        this.principal       = principal;
        this.principalLevel  = principalLevel;
        this.secondaire      = secondaire;
        this.secondaireLevel = secondaireLevel;
        this.maitrise        = maitrise;
        this.maitriseLevel   = maitriseLevel;
    }

    public static void encode(SyncCapabilityPacket pkt, FriendlyByteBuf buf) {
        buf.writeEnum(pkt.principal);
        buf.writeInt(pkt.principalLevel);
        buf.writeEnum(pkt.secondaire);
        buf.writeInt(pkt.secondaireLevel);
        buf.writeUtf(pkt.maitrise);
        buf.writeInt(pkt.maitriseLevel);
    }

    public static SyncCapabilityPacket decode(FriendlyByteBuf buf) {
        return new SyncCapabilityPacket(
            buf.readEnum(MetierPrincipal.class),
            buf.readInt(),
            buf.readEnum(MetierSecondaire.class),
            buf.readInt(),
            buf.readUtf(),
            buf.readInt()
        );
    }

    public static void handle(SyncCapabilityPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                var player = Minecraft.getInstance().player;
                if (player == null) return;
                player.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
                    cap.setPrincipal(pkt.principal);
                    cap.setPrincipalLevel(pkt.principalLevel);
                    cap.setSecondaire(pkt.secondaire);
                    cap.setSecondaireLevel(pkt.secondaireLevel);
                    cap.setMaitrise(pkt.maitrise);
                    cap.setMaitriseLevel(pkt.maitriseLevel);
                });
            })
        );
        ctx.get().setPacketHandled(true);
    }

    /** Envoie la capability du joueur vers son client (à appeler côté serveur). */
    public static void sendTo(ServerPlayer player) {
        player.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap ->
            MetierNetwork.CHANNEL.sendTo(
                new SyncCapabilityPacket(
                    cap.getPrincipal(), cap.getPrincipalLevel(),
                    cap.getSecondaire(), cap.getSecondaireLevel(),
                    cap.getMaitrise(), cap.getMaitriseLevel()
                ),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
            )
        );
    }
}
