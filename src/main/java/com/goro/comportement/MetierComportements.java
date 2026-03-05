package com.goro.comportement;

import com.goro.capability.IPlayerMetier;
import com.goro.capability.PlayerMetierProvider;
import com.goro.config.MetierLevelConfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MetierComportements {

    /** Joueurs ayant le mode bypass actif — ignorent toutes les restrictions de métier. */
    public static final Set<UUID> bypassPlayers = new HashSet<>();

    /** Mémorise le dernier joueur ayant ouvert un alambic (par position) pour contrôler le brassage. */
    private static final Map<BlockPos, UUID> brewingStandUsers = new HashMap<>();

    /** Mémorise la position de la forge Tinkers ouverte par chaque joueur (UUID → BlockPos). */
    private static final Map<UUID, BlockPos> playerSmelteryPos = new HashMap<>();

    /** Anti-spam pour les messages de restriction de fonte. */
    private static final Map<UUID, Long> smeltWarnCooldowns = new HashMap<>();

    /** IDs des blocs contrôleur de forge Tinkers' Construct. */
    private static final java.util.Set<String> SMELTERY_CONTROLLERS = java.util.Set.of(
        "tconstruct:smeltery_controller",
        "tconstruct:foundry_controller"
    );

    // ── Placement de bloc ─────────────────────────────────────────────────

    /**
     * Vérifie si l'item posé est restreint selon la config (itemRestrictions).
     */
    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getLevel().isClientSide()) return;

        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) return;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(hand.getItem());
        String message = getItemRestrictionMessage(player, itemId);
        if (message != null) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(message));
        }
    }

    // ── Casse de bloc ──────────────────────────────────────────────────────

    /**
     * Vérifie deux restrictions au moment de casser un bloc :
     *  1. Le bloc lui-même est-il réservé à un certain métier/niveau ?
     *  2. L'outil tenu en main est-il réservé à un certain métier/niveau ?
     */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getPlayer();
        if (player == null) return;

        // 1) restriction sur le bloc cassé
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(event.getState().getBlock());
        String blockMsg = getBreakRestrictionMessage(player, blockId);
        if (blockMsg != null) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(blockMsg));
            return;
        }

        // 2) restriction sur l'outil tenu en main
        ItemStack hand = player.getMainHandItem();
        if (!hand.isEmpty()) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(hand.getItem());
            String itemMsg = getItemRestrictionMessage(player, itemId);
            if (itemMsg != null) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal(itemMsg));
            }
        }
    }

    // ── Interaction avec un bloc ───────────────────────────────────────────

    /**
     * Empêche d'interagir avec un bloc (ex : alambic) si la config le restreint.
     */
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().level().isClientSide()) return;
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(
                event.getLevel().getBlockState(event.getPos()).getBlock());
        String message = getBlockInteractRestrictionMessage(event.getEntity(), blockId);
        if (message != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            event.getEntity().sendSystemMessage(Component.literal(message));
            return;
        }
        // Mémoriser qui a ouvert l'alambic pour la restriction de brassage
        if (event.getLevel().getBlockState(event.getPos()).getBlock() == Blocks.BREWING_STAND) {
            brewingStandUsers.put(event.getPos(), event.getEntity().getUUID());
        }

    }

    // ── Brassage d'une potion ──────────────────────────────────────────────

    /**
     * Chaque tick serveur : pour chaque alambic suivi, vérifie si l'ingrédient
     * en cours produirait une potion restreinte pour ce joueur.
     * Si oui, retire l'ingrédient et le rend au joueur.
     */
    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        java.util.List<BlockPos> toRemove = new java.util.ArrayList<>();
        brewingStandUsers.forEach((pos, uuid) -> {
            if (!serverLevel.hasChunkAt(pos)) return;
            if (!(serverLevel.getBlockEntity(pos) instanceof BrewingStandBlockEntity bse)) {
                toRemove.add(pos);
                return;
            }
            Player player = serverLevel.getPlayerByUUID(uuid);
            if (player == null) return;

            ItemStack ingredient = bse.getItem(3);
            if (ingredient.isEmpty()) return;

            for (int slot = 0; slot <= 2; slot++) {
                ItemStack input = bse.getItem(slot);
                if (input.isEmpty()) continue;
                if (!PotionBrewing.hasMix(input, ingredient)) continue;

                ItemStack output = PotionBrewing.mix(ingredient, input.copy());
                ResourceLocation potionId = BuiltInRegistries.POTION.getKey(PotionUtils.getPotion(output));

                String msg = getPotionRestrictionMessage(player, potionId);
                if (msg != null) {
                    ItemStack toReturn = ingredient.copy();
                    bse.setItem(3, ItemStack.EMPTY);
                    player.getInventory().add(toReturn);
                    player.sendSystemMessage(Component.literal(msg));
                    return;
                }
            }
        });
        toRemove.forEach(brewingStandUsers::remove);

        // ── Vérification des forges Tinkers (fallback IItemHandler) ──────────
        java.util.List<UUID> smeltToRemove = new java.util.ArrayList<>();
        playerSmelteryPos.forEach((uuid, pos) -> {
            if (!serverLevel.hasChunkAt(pos)) return;
            BlockEntity sbe = serverLevel.getBlockEntity(pos);
            if (sbe == null) { smeltToRemove.add(uuid); return; }

            Player player = serverLevel.getPlayerByUUID(uuid);
            if (player == null) { smeltToRemove.add(uuid); return; }
            if (bypassPlayers.contains(uuid)) return;

            int[] playerForgeron = { 0 };
            player.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap ->
                playerForgeron[0] = getPlayerLevelForMetier(cap, "FORGERON")
            );

            sbe.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)
               .ifPresent(handler -> {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack stack = handler.getStackInSlot(slot);
                    if (stack.isEmpty()) continue;
                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (itemId == null) continue;
                    int required = MetierLevelConfig.getSmeltLevelForItem(itemId);
                    if (required <= 0 || playerForgeron[0] >= required) continue;

                    ItemStack ejected = handler.extractItem(slot, stack.getCount(), false);
                    if (ejected.isEmpty()) continue;
                    if (!player.addItem(ejected)) {
                        serverLevel.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                            serverLevel, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, ejected));
                    }
                    long now = System.currentTimeMillis();
                    if (now - smeltWarnCooldowns.getOrDefault(player.getUUID(), 0L) > 2000L) {
                        smeltWarnCooldowns.put(player.getUUID(), now);
                        player.sendSystemMessage(Component.literal(
                            "§cFonte réservée — requis : §eFORGERON niv. " + required));
                    }
                }
            });
        });
        smeltToRemove.forEach(playerSmelteryPos::remove);
    }

    // ── Ouverture/fermeture du menu de forge Tinkers ───────────────────────

    /**
     * Quand un joueur ouvre un menu de smeltery/fonderie Tinkers :
     *  1. On trouve le bloc contrôleur le plus proche et on mémorise UUID → BlockPos.
     *  2. On ajoute un écouteur de slot direct : si un item restreint est placé, on l'éjecte.
     */
    @SubscribeEvent
    public void onSmelteryOpen(PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        AbstractContainerMenu container = event.getContainer();
        String cls = container.getClass().getName();
        boolean isTinkers = cls.startsWith("slimeknights.tconstruct") &&
            (cls.contains("Smeltery") || cls.contains("Foundry") || cls.contains("Melting")
             || cls.toLowerCase().contains("smeltery") || cls.toLowerCase().contains("foundry"));
        if (!isTinkers) return;

        // Trouver le contrôleur le plus proche (max 12 blocs)
        BlockPos playerPos = player.blockPosition();
        BlockPos foundPos = null;
        search:
        for (int dx = -12; dx <= 12; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -12; dz <= 12; dz++) {
                    BlockPos check = playerPos.offset(dx, dy, dz);
                    ResourceLocation bid = ForgeRegistries.BLOCKS.getKey(
                        player.level().getBlockState(check).getBlock());
                    if (bid != null && SMELTERY_CONTROLLERS.contains(bid.toString())) {
                        foundPos = check;
                        break search;
                    }
                }
            }
        }
        if (foundPos != null) playerSmelteryPos.put(player.getUUID(), foundPos);

        // Écouteur direct : éjecte les items restreints dès leur dépôt dans un slot
        final Player fp = player;
        container.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu menu, int slotIdx, ItemStack stack) {
                if (stack.isEmpty()) return;
                if (bypassPlayers.contains(fp.getUUID())) return;
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (itemId == null) return;
                int required = MetierLevelConfig.getSmeltLevelForItem(itemId);
                if (required <= 0) return;

                int[] forgeronLevel = { 0 };
                fp.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap ->
                    forgeronLevel[0] = getPlayerLevelForMetier(cap, "FORGERON")
                );
                if (forgeronLevel[0] >= required) return;

                // Éjecter l'item du slot
                Slot slot = menu.getSlot(slotIdx);
                ItemStack toEject = slot.getItem().copy();
                if (toEject.isEmpty()) return;
                slot.set(ItemStack.EMPTY);
                slot.setChanged();
                if (!fp.addItem(toEject)) {
                    fp.drop(toEject, false);
                }
                long now = System.currentTimeMillis();
                if (now - smeltWarnCooldowns.getOrDefault(fp.getUUID(), 0L) > 2000L) {
                    smeltWarnCooldowns.put(fp.getUUID(), now);
                    fp.sendSystemMessage(Component.literal(
                        "§cFonte réservée — requis : §eFORGERON niv. " + required));
                }
            }
            @Override
            public void dataChanged(AbstractContainerMenu c, int idx, int val) {}
        });
    }

    @SubscribeEvent
    public void onSmelteryClose(PlayerContainerEvent.Close event) {
        playerSmelteryPos.remove(event.getEntity().getUUID());
    }

    // ── Utilisation d'item dans l'air ──────────────────────────────────────

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide()) return;
        ItemStack stack = event.getItemStack();

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String message = getItemRestrictionMessage(event.getEntity(), itemId);
        if (message != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            event.getEntity().sendSystemMessage(Component.literal(message));
            return;
        }

        // Restriction canne à pêche (tous les types, y compris mods étendant FishingRodItem)
        if (stack.getItem() instanceof FishingRodItem) {
            Map<String, Integer> restrictions = MetierLevelConfig.getFishingRestrictions();
            if (!restrictions.isEmpty()) {
                String msg = buildRestrictionMessage(event.getEntity(), restrictions, "Pêche réservée");
                if (msg != null) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.CONSUME);
                    event.getEntity().sendSystemMessage(Component.literal(msg));
                }
            }
        }
    }

    // ── Attaque avec un item ───────────────────────────────────────────────

    /**
     * Empêche d'attaquer avec un item (hache, épée…) si celui-ci est restreint.
     */
    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        ItemStack hand = event.getEntity().getMainHandItem();
        if (hand.isEmpty()) return;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(hand.getItem());
        String message = getItemRestrictionMessage(event.getEntity(), itemId);
        if (message != null) {
            event.setCanceled(true);
            event.getEntity().sendSystemMessage(Component.literal(message));
        }
    }

    // ── Craft d'item ───────────────────────────────────────────────────────

    /**
     * Empêche de conserver un item crafté si le craft est restreint.
     * L'item est détruit (pas droppé — sinon le joueur pourrait le ramasser).
     * Les matériaux de craft sont déjà consommés, c'est la pénalité.
     */
    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Player player = event.getEntity();
        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty()) return;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(crafted.getItem());
        Map<String, Integer> restrictions = MetierLevelConfig.getCraftRestrictionsForItem(itemId);
        if (restrictions.isEmpty()) return;

        String message = buildRestrictionMessage(player, restrictions, "Craft réservé");
        if (message != null) {
            // Curseur (clic normal) : shrink détruit l'item sans le dropper
            ItemStack carried = player.containerMenu.getCarried();
            if (!carried.isEmpty() && carried.getItem() == crafted.getItem()) {
                carried.shrink(crafted.getCount());
            } else {
                // Inventaire (shift-clic) : détruire silencieusement
                Inventory inv = player.getInventory();
                int remaining = crafted.getCount();
                for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
                    ItemStack slot = inv.getItem(i);
                    if (!slot.isEmpty() && slot.getItem() == crafted.getItem()) {
                        int take = Math.min(slot.getCount(), remaining);
                        slot.shrink(take);
                        remaining -= take;
                    }
                }
            }
            player.sendSystemMessage(Component.literal(message));
        }
    }

    // ── Reproduction des animaux ───────────────────────────────────────────

    /**
     * Seul un joueur avec le métier FERMIER (principal, secondaire ou maîtrise)
     * peut faire reproduire mouton, cheval, chien, cochon, lapin, vache ou poulet.
     * On intercepte l'interaction avant que l'item soit consommé.
     */
    @SubscribeEvent
    public void onPlayerInteractAnimal(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getTarget() instanceof Animal animal)) return;

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
        Map<String, Integer> restrictions = MetierLevelConfig.getBreedRestrictionsForEntity(entityId);
        if (restrictions.isEmpty()) return;

        // Apprivoisement (clic droit sur un animal apprivoisable non encore apprivoisé)
        if (animal instanceof TamableAnimal tamable && !tamable.isTame()) {
            String message = buildRestrictionMessage(event.getEntity(), restrictions, "Apprivoisement réservé");
            if (message != null) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
                event.getEntity().sendSystemMessage(Component.literal(message));
                return;
            }
        }

        // Reproduction (nourrir un animal avec sa nourriture préférée)
        if (animal.isFood(event.getItemStack()) && animal.canFallInLove()) {
            String message = buildRestrictionMessage(event.getEntity(), restrictions, "Élevage réservé");
            if (message != null) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
                event.getEntity().sendSystemMessage(Component.literal(message));
            }
        }
    }

    /**
     * Intercepte tout apprivoisement quelle que soit la méthode (clic droit ou item dropé —
     * Alex's Mobs appelle TamableAnimal.tame() qui fire cet event).
     * Filet de sécurité : l'item peut déjà être consommé à ce stade.
     */
    @SubscribeEvent
    public void onAnimalTame(AnimalTameEvent event) {
        Player player = event.getTamer();
        if (player == null || player.level().isClientSide()) return;

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(event.getAnimal().getType());
        Map<String, Integer> restrictions = MetierLevelConfig.getBreedRestrictionsForEntity(entityId);
        if (restrictions.isEmpty()) return;

        String message = buildRestrictionMessage(player, restrictions, "Apprivoisement réservé");
        if (message != null) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(message + " §7(item perdu)"));
        }
    }

    /**
     * Couche secondaire pour les mods qui utilisent les items jetés pour reproduire
     * (ex: Alex's Mobs). Pour les animaux vanilla, onPlayerInteractAnimal bloque déjà
     * avant la consommation de l'item. Ici l'item est déjà consommé : la pénalité est voulue.
     */
    @SubscribeEvent
    public void onBabySpawn(BabyEntitySpawnEvent event) {
        Player player = event.getCausedByPlayer();
        if (player == null || player.level().isClientSide()) return;
        if (!(event.getParentA() instanceof Animal)) return;

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(event.getParentA().getType());
        Map<String, Integer> restrictions = MetierLevelConfig.getBreedRestrictionsForEntity(entityId);
        if (restrictions.isEmpty()) return;

        String message = buildRestrictionMessage(player, restrictions, "Élevage réservé");
        if (message != null) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(message + " §7(item perdu)"));
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Retourne un message d'erreur si l'item est restreint, null sinon. */
    private String getItemRestrictionMessage(Player player, ResourceLocation itemId) {
        if (itemId == null) return null;
        Map<String, Integer> restrictions = MetierLevelConfig.getRestrictionsForItem(itemId);
        if (restrictions.isEmpty()) return null;
        return buildRestrictionMessage(player, restrictions, "Item réservé");
    }

    /** Retourne un message d'erreur si le bloc est restreint à la casse, null sinon. */
    private String getBreakRestrictionMessage(Player player, ResourceLocation blockId) {
        if (blockId == null) return null;
        Map<String, Integer> restrictions = MetierLevelConfig.getBreakRestrictionsForBlock(blockId);
        if (restrictions.isEmpty()) return null;
        return buildRestrictionMessage(player, restrictions, "Bloc réservé");
    }

    /** Retourne un message d'erreur si l'interaction avec le bloc est restreinte, null sinon. */
    private String getBlockInteractRestrictionMessage(Player player, ResourceLocation blockId) {
        if (blockId == null) return null;
        Map<String, Integer> restrictions = MetierLevelConfig.getBlockInteractRestrictionsForBlock(blockId);
        if (restrictions.isEmpty()) return null;
        return buildRestrictionMessage(player, restrictions, "Interaction réservée");
    }

    /** Retourne un message d'erreur si ce type de potion est restreint, null sinon. */
    private String getPotionRestrictionMessage(Player player, ResourceLocation potionId) {
        if (potionId == null) return null;
        Map<String, Integer> restrictions = MetierLevelConfig.getPotionRestrictionsForPotion(potionId);
        if (restrictions.isEmpty()) return null;
        return buildRestrictionMessage(player, restrictions, "Potion réservée");
    }

    /**
     * Logique commune : le joueur peut utiliser/casser si au moins une des conditions est remplie.
     * Retourne null si OK, sinon un message listant ce qui est requis.
     */
    private String buildRestrictionMessage(Player player,
                                           Map<String, Integer> restrictions,
                                           String prefix) {
        if (bypassPlayers.contains(player.getUUID())) return null;
        String[] result = {null};
        player.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
            boolean canUse = false;
            StringBuilder needed = new StringBuilder();

            for (Map.Entry<String, Integer> entry : restrictions.entrySet()) {
                int playerLevel = getPlayerLevelForMetier(cap, entry.getKey());
                if (playerLevel >= entry.getValue()) {
                    canUse = true;
                    break;
                }
                if (needed.length() > 0) needed.append(" ou ");
                needed.append(entry.getKey()).append(" niv.").append(entry.getValue());
            }

            if (!canUse) result[0] = prefix + " — requis : " + needed;
        });
        return result[0];
    }

    /** Retourne le niveau effectif du joueur pour un métier donné (principal, secondaire, maîtrise ou base). */
    private int getPlayerLevelForMetier(IPlayerMetier cap, String metierName) {
        // Métiers de base universels — tout le monde en a un niveau
        if (metierName.equals("MINEUR"))   return cap.getMineurLevel();
        if (metierName.equals("BUCHERON")) return cap.getBucheronLevel();

        int level = 0;
        if (cap.getPrincipal().name().equals(metierName))
            level = Math.max(level, cap.getPrincipalLevel());
        if (cap.getSecondaire().name().equals(metierName))
            level = Math.max(level, cap.getSecondaireLevel());
        // Métier de maîtrise : utilise son propre niveau
        if (!cap.getMaitrise().isEmpty() && cap.getMaitrise().equals(metierName))
            level = Math.max(level, cap.getMaitriseLevel());
        // Si le joueur a obtenu une maîtrise issue de ce métier, il en débloque tous les niveaux
        if (!cap.getMaitrise().isEmpty()
                && MetierLevelConfig.getMaitriseOptions(metierName).contains(cap.getMaitrise()))
            level = Math.max(level, MetierLevelConfig.getMaxLevel(metierName));
        return level;
    }
}
