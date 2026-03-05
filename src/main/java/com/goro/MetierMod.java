package com.goro;

import com.goro.capability.IPlayerMetier;
import com.goro.capability.PlayerMetierEvents;
import com.goro.commande.MetierCommande;
import com.goro.comportement.MetierChatHandler;
import com.goro.comportement.MetierComportements;
import com.goro.comportement.MetierHuntingHandler;
import com.goro.config.MetierLevelConfig;
import com.goro.network.MetierNetwork;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MetierMod.MODID)
public class MetierMod {

    public static final String MODID = "metiermod";

    public MetierMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onRegisterCapabilities);

        MetierNetwork.register();

        MinecraftForge.EVENT_BUS.register(new PlayerMetierEvents());
        MinecraftForge.EVENT_BUS.register(new MetierComportements());
        MinecraftForge.EVENT_BUS.register(new MetierChatHandler());
        MinecraftForge.EVENT_BUS.register(new MetierHuntingHandler());
        MinecraftForge.EVENT_BUS.register(this);
        System.out.println("MetierMod chargé !");
    }

    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerMetier.class);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MetierLevelConfig.load();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MetierCommande.register(event.getDispatcher());
    }
}