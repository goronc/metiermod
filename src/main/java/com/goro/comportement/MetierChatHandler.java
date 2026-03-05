package com.goro.comportement;

import com.goro.capability.PlayerMetierProvider;
import com.goro.config.MetierLevelConfig;
import com.goro.data.MetierPrincipal;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MetierChatHandler {

    /**
     * Remplace le message vanilla par "[METIER] pseudo : message".
     * La couleur dépend du métier principal. En annulant l'événement,
     * la couleur de l'équipe scoreboard (bleue par défaut) est totalement ignorée.
     */
    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player.getServer() == null) return;

        String rawText = event.getRawText();

        // Priorité d'affichage : maîtrise > principal > secondaire > "Citoyen"
        String[] labelHolder = { "Citoyen" };
        String[] colorKey    = { "AUCUN" };
        player.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
            String maitrise = cap.getMaitrise();
            MetierPrincipal principal = cap.getPrincipal();
            MetierPrincipal secondaire = cap.getSecondaire();

            if (!maitrise.isEmpty()) {
                labelHolder[0] = maitrise;
                colorKey[0]    = maitrise;
            } else if (principal != MetierPrincipal.AUCUN) {
                labelHolder[0] = principal.name();
                colorKey[0]    = principal.name();
            } else if (secondaire != MetierPrincipal.AUCUN) {
                labelHolder[0] = secondaire.name();
                colorKey[0]    = secondaire.name();
            }
        });
        String label = labelHolder[0];
        int color    = MetierLevelConfig.getChatColor(colorKey[0]);

        Component full = Component.empty()
            .append(Component.literal("[" + label + "] ")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(false)))
            .append(Component.literal(player.getGameProfile().getName())
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))))
            .append(Component.literal(" : ")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x888888))))
            .append(Component.literal(rawText)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xEEEEEE))));

        // Annule le chat vanilla (et donc la couleur de l'équipe) et diffuse notre version
        event.setCanceled(true);
        player.getServer().getPlayerList().getPlayers()
            .forEach(p -> p.sendSystemMessage(full));
    }


}
