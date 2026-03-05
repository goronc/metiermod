package com.goro.capability;

import com.goro.network.SyncCapabilityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerMetierEvents {

    public static final ResourceLocation ID =
            new ResourceLocation("metiermod:metier");

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) return;
        event.addCapability(ID, new PlayerMetierProvider());
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SyncCapabilityPacket.sendTo(player);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        Player original = event.getOriginal();
        original.reviveCaps();
        original.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(oldCap ->
            newPlayer.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(newCap -> {
                newCap.setPrincipal(oldCap.getPrincipal());
                newCap.setPrincipalLevel(oldCap.getPrincipalLevel());
                newCap.setSecondaire(oldCap.getSecondaire());
                newCap.setSecondaireLevel(oldCap.getSecondaireLevel());
                newCap.setMaitrise(oldCap.getMaitrise());
                newCap.setMaitriseLevel(oldCap.getMaitriseLevel());
            })
        );
        original.invalidateCaps();
        SyncCapabilityPacket.sendTo(newPlayer);
    }
}
