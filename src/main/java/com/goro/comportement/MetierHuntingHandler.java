package com.goro.comportement;

import com.goro.capability.IPlayerMetier;
import com.goro.capability.PlayerMetierProvider;
import com.goro.config.MetierLevelConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MetierHuntingHandler {

    /** Cooldown anti-spam : évite de spammer le message à chaque tick. */
    private final Map<UUID, Long> messageCooldowns = new HashMap<>();

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player) return; // Pas de restriction PvP
        if (event.getEntity().level().isClientSide()) return;

        // L'attaquant doit être un joueur (direct ou indirect — couvre les flèches)
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        // Bypass admin : aucune restriction
        if (MetierComportements.bypassPlayers.contains(player.getUUID())) return;

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (entityId == null) return;

        int requiredLevel = MetierLevelConfig.getHuntingLevelForEntity(entityId);
        if (requiredLevel <= 0) return; // Pas de restriction pour cette entité

        int chasseurLevel = getChasseurLevel(player);
        if (chasseurLevel >= requiredLevel) return; // Joueur autorisé

        event.setCanceled(true);

        // Anti-spam : message max toutes les 2 secondes
        long now = System.currentTimeMillis();
        Long last = messageCooldowns.get(player.getUUID());
        if (last == null || now - last > 2000L) {
            messageCooldowns.put(player.getUUID(), now);
            player.sendSystemMessage(Component.literal(
                "§cChasse réservée — requis : §eCHASSEUR niv. " + requiredLevel));
        }
    }

    private int getChasseurLevel(Player player) {
        int[] level = { 0 };
        player.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
            level[0] = getCapChasseurLevel(cap);
        });
        return level[0];
    }

    private int getCapChasseurLevel(IPlayerMetier cap) {
        int level = 0;
        if (cap.getPrincipal().name().equals("CHASSEUR"))
            level = Math.max(level, cap.getPrincipalLevel());
        if (cap.getSecondaire().name().equals("CHASSEUR"))
            level = Math.max(level, cap.getSecondaireLevel());
        if (!cap.getMaitrise().isEmpty() && cap.getMaitrise().equals("CHASSEUR"))
            level = Math.max(level, cap.getMaitriseLevel());
        // Si le joueur a une maîtrise issue de CHASSEUR, il débloque tous les niveaux
        if (!cap.getMaitrise().isEmpty()
                && MetierLevelConfig.getMaitriseOptions("CHASSEUR").contains(cap.getMaitrise()))
            level = Math.max(level, MetierLevelConfig.getMaxLevel("CHASSEUR"));
        return level;
    }
}
