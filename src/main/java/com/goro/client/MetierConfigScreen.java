package com.goro.client;

import com.goro.config.MetierLevelConfig;
import com.goro.data.MetierPrincipal;
import com.goro.data.MetierSecondaire;
import com.goro.network.MetierNetwork;
import com.goro.network.SaveConfigPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class MetierConfigScreen extends Screen {

    private enum Section  { ITEMS, NIVEAUX, MAITRISE, ELEVAGE, POTIONS, PECHE, CHASSE, FORGE, COULEURS }
    private enum ItemMode { USE, BREAK, CRAFT, INTERACT }
    private enum SortMode { ALPHA_ASC, ALPHA_DESC, LEVEL_DESC }

    // ── Dimensions ─────────────────────────────────────────────────────────
    private static final int PANEL_W    = 340;
    private static final int PANEL_H    = 260;
    private static final int COL        = 9;
    private static final int CELL       = 18;
    private static final int ROWS       = 4;
    private static final int MOD_Y_OFF  = 53;  // offset depuis py pour la bande mod
    private static final int MOD_H      = 12;
    private static final int GRID_Y_OFF = 70;  // offset depuis py pour la grille d'items
    private static final int SORT_BTN_W = 34;

    // ── Potions vanilla (35 types) ─────────────────────────────────────────
    private static final ResourceLocation[] POTION_IDS = {
        new ResourceLocation("minecraft", "night_vision"),
        new ResourceLocation("minecraft", "long_night_vision"),
        new ResourceLocation("minecraft", "invisibility"),
        new ResourceLocation("minecraft", "long_invisibility"),
        new ResourceLocation("minecraft", "leaping"),
        new ResourceLocation("minecraft", "long_leaping"),
        new ResourceLocation("minecraft", "strong_leaping"),
        new ResourceLocation("minecraft", "fire_resistance"),
        new ResourceLocation("minecraft", "long_fire_resistance"),
        new ResourceLocation("minecraft", "swiftness"),
        new ResourceLocation("minecraft", "long_swiftness"),
        new ResourceLocation("minecraft", "strong_swiftness"),
        new ResourceLocation("minecraft", "slowness"),
        new ResourceLocation("minecraft", "long_slowness"),
        new ResourceLocation("minecraft", "strong_slowness"),
        new ResourceLocation("minecraft", "water_breathing"),
        new ResourceLocation("minecraft", "long_water_breathing"),
        new ResourceLocation("minecraft", "healing"),
        new ResourceLocation("minecraft", "strong_healing"),
        new ResourceLocation("minecraft", "harming"),
        new ResourceLocation("minecraft", "strong_harming"),
        new ResourceLocation("minecraft", "poison"),
        new ResourceLocation("minecraft", "long_poison"),
        new ResourceLocation("minecraft", "strong_poison"),
        new ResourceLocation("minecraft", "regeneration"),
        new ResourceLocation("minecraft", "long_regeneration"),
        new ResourceLocation("minecraft", "strong_regeneration"),
        new ResourceLocation("minecraft", "strength"),
        new ResourceLocation("minecraft", "long_strength"),
        new ResourceLocation("minecraft", "strong_strength"),
        new ResourceLocation("minecraft", "weakness"),
        new ResourceLocation("minecraft", "long_weakness"),
        new ResourceLocation("minecraft", "luck"),
        new ResourceLocation("minecraft", "slow_falling"),
        new ResourceLocation("minecraft", "long_slow_falling"),
    };
    private static final String[] POTION_NAMES = {
        "Vision nocturne", "Vision nocturne (long)", "Invisibilité", "Invisibilité (long)",
        "Saut", "Saut (long)", "Saut II",
        "Résistance au feu", "Résistance au feu (long)",
        "Rapidité", "Rapidité (long)", "Rapidité II",
        "Lenteur", "Lenteur (long)", "Lenteur II",
        "Respiration aquatique", "Respiration aquatique (long)",
        "Soin", "Soin II", "Dommages", "Dommages II",
        "Poison", "Poison (long)", "Poison II",
        "Régénération", "Régénération (long)", "Régénération II",
        "Force", "Force (long)", "Force II",
        "Faiblesse", "Faiblesse (long)",
        "Chance", "Chute lente", "Chute lente (long)"
    };

    // ── Animaux reproductibles (tous ceux de vanilla 1.20) ──────────────────
    private static final ResourceLocation[] BREED_ENTITIES = {
        new ResourceLocation("minecraft", "sheep"),
        new ResourceLocation("minecraft", "cow"),
        new ResourceLocation("minecraft", "mooshroom"),
        new ResourceLocation("minecraft", "pig"),
        new ResourceLocation("minecraft", "chicken"),
        new ResourceLocation("minecraft", "rabbit"),
        new ResourceLocation("minecraft", "wolf"),
        new ResourceLocation("minecraft", "cat"),
        new ResourceLocation("minecraft", "ocelot"),
        new ResourceLocation("minecraft", "horse"),
        new ResourceLocation("minecraft", "donkey"),
        new ResourceLocation("minecraft", "llama"),
        new ResourceLocation("minecraft", "goat"),
        new ResourceLocation("minecraft", "axolotl"),
        new ResourceLocation("minecraft", "turtle"),
        new ResourceLocation("minecraft", "fox"),
        new ResourceLocation("minecraft", "panda"),
        new ResourceLocation("minecraft", "bee"),
        new ResourceLocation("minecraft", "hoglin"),
        new ResourceLocation("minecraft", "strider"),
        new ResourceLocation("minecraft", "frog"),
        new ResourceLocation("minecraft", "sniffer"),
        new ResourceLocation("minecraft", "camel"),
    };
    private static final String[] BREED_NAMES = {
        "Mouton", "Vache", "Vache Champignon", "Cochon", "Poulet", "Lapin",
        "Loup/Chien", "Chat", "Ocelot", "Cheval", "Âne", "Lama",
        "Chèvre", "Axolotl", "Tortue", "Renard", "Panda", "Abeille",
        "Hoglin", "Arpenteur", "Grenouille", "Renifleur", "Chameau"
    };

    // ── Edit data ───────────────────────────────────────────────────────────
    private Map<String, Map<ResourceLocation, Integer>> editItems;
    private Map<String, Map<ResourceLocation, Integer>> editBreaks;
    private Map<String, Map<ResourceLocation, Integer>> editCraft;
    private Map<String, Map<ResourceLocation, Integer>> editBreeds;
    private Map<String, Map<ResourceLocation, Integer>> editBlockInteract;
    private Map<String, Map<ResourceLocation, Integer>> editPotions;
    private Map<String, Integer>                        editMaxLevels;
    private Map<String, List<String>>                   editMaitrise;
    private Map<String, Integer>                        editChatColors;
    private Map<String, Integer>                        editFishing;
    private Map<ResourceLocation, Integer>              editHunting;
    private Map<ResourceLocation, Integer>              editSmelt;

    // ── Tabs ────────────────────────────────────────────────────────────────
    private final List<String> tabs       = new ArrayList<>();
    private       String       activeMetier;
    private final List<Button> tabButtons = new ArrayList<>();

    // ── Section / mode / sort ────────────────────────────────────────────────
    private Section              activeSection = Section.ITEMS;
    private ItemMode             itemMode      = ItemMode.USE;
    private SortMode             sortMode      = SortMode.ALPHA_ASC;
    private final List<AbstractWidget> sectionWidgets = new ArrayList<>();

    // ── Items section ───────────────────────────────────────────────────────
    private EditBox    searchBox;
    private List<Item> allItems      = new ArrayList<>();
    private List<Item> allBlockItems = new ArrayList<>();
    private List<Item> filteredItems = new ArrayList<>();
    private int        itemScroll     = 0;
    private int        niveauxScroll  = 0;
    private int        maitriseScroll = 0;
    private int        breedScroll    = 0;
    private int        potionScroll   = 0;
    private int        fishingScroll  = 0;
    private int        colorScroll    = 0;

    // ── Chasse section ──────────────────────────────────────────────────────
    private int                    chasseScroll      = 0;
    private EditBox                chasseSearchBox;
    private String                 chasseSearchQuery = "";
    private List<ResourceLocation> allEntityIds      = new ArrayList<>();
    private List<ResourceLocation> filteredEntityIds = new ArrayList<>();
    private List<String>           chasseNamespaces  = new ArrayList<>();
    private String                 chasseNamespace   = null;
    private int                    chasseModScrollX  = 0;

    // ── Forge section ───────────────────────────────────────────────────────
    private int                    forgeScroll       = 0;
    private EditBox                forgeSearchBox;
    private String                 forgeSearchQuery  = "";
    private List<ResourceLocation> allSmeltItemIds   = new ArrayList<>();
    private List<ResourceLocation> filteredSmeltIds  = new ArrayList<>();
    private List<String>           forgeNamespaces   = new ArrayList<>();
    private String                 forgeNamespace    = null;
    private int                    forgeModScrollX   = 0;

    // ── Filtre par mod ──────────────────────────────────────────────────────
    private List<String> allNamespaces   = new ArrayList<>();
    private String       activeNamespace = null;  // null = tous les mods
    private int          modScrollX      = 0;

    // ── Panel origin ────────────────────────────────────────────────────────
    private int px, py;

    // ───────────────────────────────────────────────────────────────────────

    public MetierConfigScreen() {
        super(Component.literal("Configuration des Métiers"));

        for (MetierPrincipal  m : MetierPrincipal.values())
            if (m != MetierPrincipal.AUCUN) tabs.add(m.name());
        for (MetierSecondaire m : MetierSecondaire.values())
            if (m != MetierSecondaire.AUCUN && !tabs.contains(m.name())) tabs.add(m.name());

        editItems        = deepCopy(MetierLevelConfig.getAll());
        editBreaks       = deepCopy(MetierLevelConfig.getAllBreakRestrictions());
        editCraft        = deepCopy(MetierLevelConfig.getAllCraftRestrictions());
        editBreeds       = deepCopy(MetierLevelConfig.getAllBreedRestrictions());
        editBlockInteract = deepCopy(MetierLevelConfig.getAllBlockInteractRestrictions());
        editPotions      = deepCopy(MetierLevelConfig.getAllPotionRestrictions());
        editChatColors   = new HashMap<>(MetierLevelConfig.getAllChatColors());
        editFishing      = new HashMap<>(MetierLevelConfig.getFishingRestrictions());
        editHunting      = new HashMap<>(MetierLevelConfig.getHuntingRestrictions());
        editSmelt        = new HashMap<>(MetierLevelConfig.getSmeltRestrictions());
        editMaxLevels    = new HashMap<>(MetierLevelConfig.getMaxLevels());
        editMaitrise  = new HashMap<>();
        MetierLevelConfig.getMaitrise().forEach((k, v) -> editMaitrise.put(k, new ArrayList<>(v)));

        for (List<String> opts : editMaitrise.values())
            for (String matrName : opts)
                if (!matrName.isEmpty() && !tabs.contains(matrName)) tabs.add(matrName);

        activeMetier = tabs.isEmpty() ? "" : tabs.get(0);
    }

    // ───────────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        int tabH = 14, tabGap = 2;

        // ── Pass 1 : compter les lignes d'onglets (largeurs dynamiques) ──
        int curX = 0, tabRows = 1;
        for (String tab : tabs) {
            int bw = this.font.width(tab) + 8;
            if (curX > 0 && curX + bw > PANEL_W) { tabRows++; curX = 0; }
            curX += bw + tabGap;
        }

        int totalH = PANEL_H + tabRows * (tabH + tabGap);
        px = (this.width  - PANEL_W) / 2;
        py = (this.height - totalH)  / 2 + tabRows * (tabH + tabGap);

        // ── Pass 2 : créer les onglets ────────────────────────────────────
        tabButtons.clear();
        curX = 0;
        int curRow = 0;
        for (String tab : tabs) {
            int bw = this.font.width(tab) + 8;
            if (curX > 0 && curX + bw > PANEL_W) { curRow++; curX = 0; }
            int bx = px + curX;
            int by = py - (tabRows - curRow) * (tabH + tabGap);
            Button btn = Button.builder(Component.literal(tab), b -> {
                activeMetier = tab;
                itemScroll = 0;
                refreshFilter();
            }).pos(bx, by).size(bw, tabH).build();
            btn.visible = (activeSection == Section.ITEMS || activeSection == Section.ELEVAGE);
            tabButtons.add(btn);
            this.addRenderableWidget(btn);
            curX += bw + tabGap;
        }

        // ── Section selector (9 sections) ─────────────────────────────────
        int sectW = (PANEL_W - 20) / 9;
        this.addRenderableWidget(Button.builder(
            Component.literal("Items"), b -> switchSection(Section.ITEMS)
        ).pos(px + 4, py + 4).size(sectW, 14).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("Niv."), b -> switchSection(Section.NIVEAUX)
        ).pos(px + 4 + (sectW + 2), py + 4).size(sectW, 14).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("Maît."), b -> switchSection(Section.MAITRISE)
        ).pos(px + 4 + (sectW + 2) * 2, py + 4).size(sectW, 14).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("Élev."), b -> switchSection(Section.ELEVAGE)
        ).pos(px + 4 + (sectW + 2) * 3, py + 4).size(sectW, 14).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("Pot."), b -> switchSection(Section.POTIONS)
        ).pos(px + 4 + (sectW + 2) * 4, py + 4).size(sectW, 14).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("Pêche"), b -> switchSection(Section.PECHE)
        ).pos(px + 4 + (sectW + 2) * 5, py + 4).size(sectW, 14).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("Chas."), b -> switchSection(Section.CHASSE)
        ).pos(px + 4 + (sectW + 2) * 6, py + 4).size(sectW, 14).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("Forge"), b -> switchSection(Section.FORGE)
        ).pos(px + 4 + (sectW + 2) * 7, py + 4).size(sectW, 14).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("Coul."), b -> switchSection(Section.COULEURS)
        ).pos(px + 4 + (sectW + 2) * 8, py + 4).size(sectW, 14).build());

        // ── Save / Cancel ─────────────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
            Component.literal("Sauvegarder"), b -> saveAndClose()
        ).pos(px + PANEL_W / 2 - 56, py + PANEL_H - 20).size(54, 16).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("Annuler"), b -> this.onClose()
        ).pos(px + PANEL_W / 2 + 2, py + PANEL_H - 20).size(54, 16).build());

        // ── Item lists ────────────────────────────────────────────────────
        allItems = ForgeRegistries.ITEMS.getValues().stream()
            .filter(item -> ForgeRegistries.ITEMS.getKey(item) != null)
            .sorted(Comparator.comparing(item -> ForgeRegistries.ITEMS.getKey(item).toString()))
            .collect(Collectors.toList());

        allBlockItems = allItems.stream()
            .filter(item -> item instanceof BlockItem)
            .collect(Collectors.toList());

        // ── Entity list for Chasse ────────────────────────────────────────
        allEntityIds = ForgeRegistries.ENTITY_TYPES.getKeys().stream()
            .filter(id -> {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(id);
                if (type == null) return false;
                MobCategory cat = type.getCategory();
                return (cat == MobCategory.MONSTER
                     || cat == MobCategory.CREATURE
                     || cat == MobCategory.AMBIENT
                     || cat == MobCategory.WATER_CREATURE
                     || cat == MobCategory.WATER_AMBIENT
                     || cat == MobCategory.UNDERGROUND_WATER_CREATURE
                     || cat == MobCategory.AXOLOTLS)
                    && !id.toString().equals("minecraft:player");
            })
            .sorted(Comparator.comparing(ResourceLocation::toString))
            .collect(Collectors.toList());

        chasseNamespaces = allEntityIds.stream()
            .map(ResourceLocation::getNamespace)
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        // ── Item list for Forge ───────────────────────────────────────────
        allSmeltItemIds = ForgeRegistries.ITEMS.getKeys().stream()
            .filter(id -> !id.toString().equals("minecraft:air"))
            .sorted(Comparator.comparing(ResourceLocation::toString))
            .collect(Collectors.toList());

        forgeNamespaces = allSmeltItemIds.stream()
            .map(ResourceLocation::getNamespace)
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        buildSectionWidgets();
    }

    // ── Section switching ──────────────────────────────────────────────────

    private void switchSection(Section s) {
        sectionWidgets.forEach(this::removeWidget);
        sectionWidgets.clear();
        activeSection = s;
        itemScroll = 0; niveauxScroll = 0; maitriseScroll = 0;
        breedScroll = 0; potionScroll = 0; fishingScroll = 0; colorScroll = 0;
        chasseScroll = 0; chasseSearchQuery = ""; chasseNamespace = null; chasseModScrollX = 0;
        tabButtons.forEach(btn -> btn.visible =
            (s == Section.ITEMS || s == Section.ELEVAGE || s == Section.POTIONS));
        buildSectionWidgets();
    }

    private void rebuildSectionWidgets() {
        sectionWidgets.forEach(this::removeWidget);
        sectionWidgets.clear();
        buildSectionWidgets();
    }

    private void buildSectionWidgets() {
        if (activeSection == Section.ITEMS) {
            // 4 boutons de mode
            int modeW = (PANEL_W - 14) / 4;
            addAndTrack(Button.builder(Component.literal("Utiliser"), b -> {
                itemMode = ItemMode.USE; itemScroll = 0; activeNamespace = null;
                rebuildNamespaces(); refreshFilter();
            }).pos(px + 4, py + 22).size(modeW, 12).build());
            addAndTrack(Button.builder(Component.literal("Casser blocs"), b -> {
                itemMode = ItemMode.BREAK; itemScroll = 0; activeNamespace = null;
                rebuildNamespaces(); refreshFilter();
            }).pos(px + 6 + modeW, py + 22).size(modeW, 12).build());
            addAndTrack(Button.builder(Component.literal("Craft"), b -> {
                itemMode = ItemMode.CRAFT; itemScroll = 0; activeNamespace = null;
                rebuildNamespaces(); refreshFilter();
            }).pos(px + 8 + modeW * 2, py + 22).size(modeW, 12).build());
            addAndTrack(Button.builder(Component.literal("Interagir"), b -> {
                itemMode = ItemMode.INTERACT; itemScroll = 0; activeNamespace = null;
                rebuildNamespaces(); refreshFilter();
            }).pos(px + 10 + modeW * 3, py + 22).size(modeW, 12).build());

            // Barre de recherche
            searchBox = new EditBox(this.font, px + 4, py + 37, PANEL_W - 8, 14, Component.literal(""));
            searchBox.setHint(Component.literal("Rechercher..."));
            searchBox.setResponder(str -> { itemScroll = 0; refreshFilter(); });
            addAndTrack(searchBox);

            rebuildNamespaces();
            refreshFilter();

        } else if (activeSection == Section.NIVEAUX) {
            int y0 = py + 24, rowH = 22;
            for (int i = niveauxScroll; i < tabs.size(); i++) {
                String tab = tabs.get(i);
                int y = y0 + (i - niveauxScroll) * rowH;
                if (y + 16 > py + PANEL_H - 25) break;
                addAndTrack(Button.builder(Component.literal("-"),
                    b -> editMaxLevels.put(tab, Math.max(1, editMaxLevels.getOrDefault(tab, 5) - 1))
                ).pos(px + 200, y).size(16, 16).build());
                addAndTrack(Button.builder(Component.literal("+"),
                    b -> editMaxLevels.put(tab, Math.min(20, editMaxLevels.getOrDefault(tab, 5) + 1))
                ).pos(px + 219, y).size(16, 16).build());
            }

        } else if (activeSection == Section.ELEVAGE) {
            int y0 = py + 24, rowH = 20;
            for (int i = breedScroll; i < BREED_ENTITIES.length; i++) {
                ResourceLocation eid = BREED_ENTITIES[i];
                int y = y0 + (i - breedScroll) * rowH;
                if (y + 14 > py + PANEL_H - 25) break;
                addAndTrack(Button.builder(Component.literal("-"),
                    b -> cycleBreedLevel(eid, -1)
                ).pos(px + 200, y).size(16, 14).build());
                addAndTrack(Button.builder(Component.literal("+"),
                    b -> cycleBreedLevel(eid, +1)
                ).pos(px + 219, y).size(16, 14).build());
            }

        } else if (activeSection == Section.POTIONS) {
            int y0 = py + 24, rowH = 20;
            for (int i = potionScroll; i < POTION_IDS.length; i++) {
                ResourceLocation pid = POTION_IDS[i];
                int y = y0 + (i - potionScroll) * rowH;
                if (y + 14 > py + PANEL_H - 25) break;
                addAndTrack(Button.builder(Component.literal("-"),
                    b -> cyclePotionLevel(pid, -1)
                ).pos(px + 200, y).size(16, 14).build());
                addAndTrack(Button.builder(Component.literal("+"),
                    b -> cyclePotionLevel(pid, +1)
                ).pos(px + 219, y).size(16, 14).build());
            }

        } else if (activeSection == Section.PECHE) {
            int y0 = py + 24, rowH = 20;
            for (int i = fishingScroll; i < tabs.size(); i++) {
                String tab = tabs.get(i);
                int y = y0 + (i - fishingScroll) * rowH;
                if (y + 14 > py + PANEL_H - 25) break;
                addAndTrack(Button.builder(Component.literal("-"),
                    b -> cycleFishingLevel(tab, -1)
                ).pos(px + 200, y).size(16, 14).build());
                addAndTrack(Button.builder(Component.literal("+"),
                    b -> cycleFishingLevel(tab, +1)
                ).pos(px + 219, y).size(16, 14).build());
            }

        } else if (activeSection == Section.CHASSE) {
            // Search box — value set BEFORE responder to avoid recursive rebuild
            chasseSearchBox = new EditBox(this.font, px + 4, py + 22, PANEL_W - 8, 14, Component.literal(""));
            chasseSearchBox.setValue(chasseSearchQuery);
            chasseSearchBox.setHint(Component.literal("Rechercher un mob (ex: alexsmobs)..."));
            chasseSearchBox.setResponder(str -> {
                chasseSearchQuery = str;
                chasseScroll = 0;
                rebuildSectionWidgets();
            });
            addAndTrack(chasseSearchBox);

            refreshEntityFilter();

            // +/- buttons for each visible entity row (list starts at py+52)
            int y0 = py + 52, rowH = 18;
            for (int i = chasseScroll; i < filteredEntityIds.size(); i++) {
                ResourceLocation eid = filteredEntityIds.get(i);
                int y = y0 + (i - chasseScroll) * rowH;
                if (y + rowH > py + PANEL_H - 50) break;
                addAndTrack(Button.builder(Component.literal("-"),
                    b -> cycleHuntingLevel(eid, -1)
                ).pos(px + 297, y).size(16, 14).build());
                addAndTrack(Button.builder(Component.literal("+"),
                    b -> cycleHuntingLevel(eid, +1)
                ).pos(px + 316, y).size(16, 14).build());
            }

        } else if (activeSection == Section.FORGE) {
            // Search box — value set BEFORE responder pour éviter la récursion
            forgeSearchBox = new EditBox(this.font, px + 4, py + 22, PANEL_W - 8, 14, Component.literal(""));
            forgeSearchBox.setValue(forgeSearchQuery);
            forgeSearchBox.setHint(Component.literal("Rechercher un item (ex: tconstruct)..."));
            forgeSearchBox.setResponder(str -> {
                forgeSearchQuery = str;
                forgeScroll = 0;
                rebuildSectionWidgets();
            });
            addAndTrack(forgeSearchBox);

            refreshSmeltFilter();

            int y0 = py + 52, rowH = 18;
            for (int i = forgeScroll; i < filteredSmeltIds.size(); i++) {
                ResourceLocation iid = filteredSmeltIds.get(i);
                int y = y0 + (i - forgeScroll) * rowH;
                if (y + rowH > py + PANEL_H - 50) break;
                addAndTrack(Button.builder(Component.literal("-"),
                    b -> cycleSmeltLevel(iid, -1)
                ).pos(px + 297, y).size(16, 14).build());
                addAndTrack(Button.builder(Component.literal("+"),
                    b -> cycleSmeltLevel(iid, +1)
                ).pos(px + 316, y).size(16, 14).build());
            }

        } else if (activeSection == Section.COULEURS) {
            int y0 = py + 24, rowH = 22;
            for (int i = colorScroll; i < tabs.size(); i++) {
                String tab = tabs.get(i);
                int y = y0 + (i - colorScroll) * rowH;
                if (y + 16 > py + PANEL_H - 25) break;
                int currentColor = editChatColors.getOrDefault(tab, MetierLevelConfig.getChatColor(tab));
                EditBox box = new EditBox(this.font, px + 220, y, 80, 16, Component.literal(""));
                box.setMaxLength(6);
                box.setValue(String.format("%06X", currentColor & 0xFFFFFF));
                box.setFilter(s -> s.matches("[0-9A-Fa-f]{0,6}"));
                box.setResponder(val -> {
                    if (val.length() == 6) {
                        try { editChatColors.put(tab, Integer.parseInt(val, 16)); }
                        catch (NumberFormatException ignored) {}
                    }
                });
                addAndTrack(box);
            }

        } else { // MAITRISE
            int y0 = py + 24, rowH = 40;
            for (int i = maitriseScroll; i < tabs.size(); i++) {
                String tab = tabs.get(i);
                int y = y0 + (i - maitriseScroll) * rowH;
                if (y + 32 > py + PANEL_H - 25) break;
                List<String> opts = editMaitrise.computeIfAbsent(tab, k -> new ArrayList<>(Arrays.asList("", "")));
                while (opts.size() < 2) opts.add("");

                EditBox box1 = new EditBox(this.font, px + 120, y, PANEL_W - 128, 16, Component.literal(""));
                box1.setMaxLength(50);
                box1.setValue(opts.get(0));
                box1.setHint(Component.literal("Spécialisation 1"));
                box1.setResponder(val -> opts.set(0, val.trim().toUpperCase()));
                addAndTrack(box1);

                EditBox box2 = new EditBox(this.font, px + 120, y + 20, PANEL_W - 128, 16, Component.literal(""));
                box2.setMaxLength(50);
                box2.setValue(opts.get(1));
                box2.setHint(Component.literal("Spécialisation 2"));
                box2.setResponder(val -> opts.set(1, val.trim().toUpperCase()));
                addAndTrack(box2);
            }
        }
    }

    private void addAndTrack(AbstractWidget w) {
        this.addRenderableWidget(w);
        sectionWidgets.add(w);
    }

    // ── Filtre par mod ─────────────────────────────────────────────────────

    private void rebuildNamespaces() {
        List<Item> source = (itemMode == ItemMode.BREAK || itemMode == ItemMode.INTERACT)
            ? allBlockItems : allItems;
        allNamespaces = source.stream()
            .map(item -> ForgeRegistries.ITEMS.getKey(item).getNamespace())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        modScrollX = 0;
        if (activeNamespace != null && !allNamespaces.contains(activeNamespace))
            activeNamespace = null;
    }

    private int totalModButtonsWidth() {
        int w = this.font.width("Tous") + 6 + 2;
        for (String ns : allNamespaces) w += this.font.width(ns) + 6 + 2;
        return w;
    }

    private int totalChasseNsWidth() {
        int w = this.font.width("Tous") + 6 + 2;
        for (String ns : chasseNamespaces) w += this.font.width(ns) + 6 + 2;
        return w;
    }

    private int totalForgeNsWidth() {
        int w = this.font.width("Tous") + 6 + 2;
        for (String ns : forgeNamespaces) w += this.font.width(ns) + 6 + 2;
        return w;
    }

    private void refreshSmeltFilter() {
        String q = forgeSearchQuery.toLowerCase();
        filteredSmeltIds = allSmeltItemIds.stream()
            .filter(id -> {
                if (forgeNamespace != null && !id.getNamespace().equals(forgeNamespace)) return false;
                return id.toString().contains(q);
            })
            .collect(Collectors.toList());
    }

    private void cycleSmeltLevel(ResourceLocation itemId, int dir) {
        int maxLvl = editMaxLevels.getOrDefault("FORGERON", 5);
        int next = Math.max(0, Math.min(maxLvl, editSmelt.getOrDefault(itemId, 0) + dir));
        if (next == 0) editSmelt.remove(itemId);
        else           editSmelt.put(itemId, next);
    }

    // ── Tri ────────────────────────────────────────────────────────────────

    private void cycleSortMode() {
        sortMode = switch (sortMode) {
            case ALPHA_ASC  -> SortMode.ALPHA_DESC;
            case ALPHA_DESC -> SortMode.LEVEL_DESC;
            default         -> SortMode.ALPHA_ASC;
        };
        itemScroll = 0;
        refreshFilter();
    }

    private Comparator<Item> getSortComparator() {
        switch (sortMode) {
            case ALPHA_DESC:
                return Comparator.comparing(
                    (Item item) -> ForgeRegistries.ITEMS.getKey(item).toString(),
                    Comparator.reverseOrder());
            case LEVEL_DESC:
                return Comparator.comparingInt(
                        (Item item) -> -getLevel(ForgeRegistries.ITEMS.getKey(item)))
                    .thenComparing(item -> ForgeRegistries.ITEMS.getKey(item).toString());
            default: // ALPHA_ASC
                return Comparator.comparing(item -> ForgeRegistries.ITEMS.getKey(item).toString());
        }
    }

    // ── Rendu ──────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.fill(px, py, px + PANEL_W, py + PANEL_H, 0xCC0D0D1F);
        g.renderOutline(px, py, PANEL_W, PANEL_H, 0xFF6060B0);
        g.fill(px + 2, py + 20, px + PANEL_W - 2, py + 21, 0xFF6060B0);

        renderButtonGlows(g, mx, my);

        if (activeSection == Section.ITEMS)         renderItemsSection(g, mx, my);
        else if (activeSection == Section.NIVEAUX)  renderNiveauxSection(g);
        else if (activeSection == Section.MAITRISE) renderMaitriseSection(g);
        else if (activeSection == Section.ELEVAGE)  renderElevageSection(g);
        else if (activeSection == Section.POTIONS)  renderPotionsSection(g);
        else if (activeSection == Section.PECHE)    renderPecheSection(g);
        else if (activeSection == Section.CHASSE)   renderChasseSection(g, mx, my);
        else if (activeSection == Section.FORGE)    renderForgeSection(g, mx, my);
        else                                        renderCouleursSection(g);

        // Crédit
        String credit = "dev by Goro";
        g.drawString(this.font, credit, px + PANEL_W - this.font.width(credit) - 4, py + PANEL_H - 10, 0xFF4444AA, false);

        super.render(g, mx, my, pt);
    }

    /** Dessine un halo lumineux autour d'un bouton (3 couches concentriques). */
    private void renderGlow(GuiGraphics g, int x, int y, int w, int h, int rgb) {
        int r = (rgb >> 16) & 0xFF, gr = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        int[] alphas = { 0x18, 0x30, 0x55 };
        int[] pads   = { 4, 2, 1 };
        for (int i = 0; i < 3; i++) {
            int p = pads[i];
            g.fill(x - p, y - p, x + w + p, y + h + p,
                   (alphas[i] << 24) | (r << 16) | (gr << 8) | b);
        }
    }

    /** Rend les halos sur les boutons actifs / survolés. */
    private void renderButtonGlows(GuiGraphics g, int mx, int my) {
        int sectW = (PANEL_W - 20) / 9;

        // ── Section selector (9 sections) ────────────────────────────────
        int[] sectX = {
            px + 4,
            px + 4 + (sectW + 2),
            px + 4 + (sectW + 2) * 2,
            px + 4 + (sectW + 2) * 3,
            px + 4 + (sectW + 2) * 4,
            px + 4 + (sectW + 2) * 5,
            px + 4 + (sectW + 2) * 6,
            px + 4 + (sectW + 2) * 7,
            px + 4 + (sectW + 2) * 8
        };
        Section[] sects = {
            Section.ITEMS, Section.NIVEAUX, Section.MAITRISE, Section.ELEVAGE,
            Section.POTIONS, Section.PECHE, Section.CHASSE, Section.FORGE, Section.COULEURS
        };
        for (int i = 0; i < 9; i++)
            if (activeSection == sects[i])
                renderGlow(g, sectX[i], py + 4, sectW, 14, 0x7070FF);

        // ── Mode buttons (ITEMS only) ─────────────────────────────────────
        if (activeSection == Section.ITEMS) {
            int modeW = (PANEL_W - 14) / 4;
            int[] modeX   = { px + 4, px + 6 + modeW, px + 8 + modeW * 2, px + 10 + modeW * 3 };
            ItemMode[] ms = { ItemMode.USE, ItemMode.BREAK, ItemMode.CRAFT, ItemMode.INTERACT };
            int[] modeRgb = { 0x4488FF, 0xFF8800, 0x44BB44, 0x00BBCC };
            for (int i = 0; i < 4; i++)
                if (itemMode == ms[i])
                    renderGlow(g, modeX[i], py + 22, modeW, 12, modeRgb[i]);
        }

        // ── Onglet actif (ITEMS, ELEVAGE et POTIONS) ──────────────────────
        if (activeSection == Section.ITEMS || activeSection == Section.ELEVAGE
                || activeSection == Section.POTIONS) {
            for (Button btn : tabButtons) {
                if (btn.getMessage().getString().equals(activeMetier)) {
                    renderGlow(g, btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight(), 0xFFDD44);
                    break;
                }
            }
        }

        // ── Save / Cancel hover ───────────────────────────────────────────
        int saveX   = px + PANEL_W / 2 - 56;
        int cancelX = px + PANEL_W / 2 + 2;
        int saveY   = py + PANEL_H - 20;
        if (mx >= saveX && mx < saveX + 54 && my >= saveY && my < saveY + 16)
            renderGlow(g, saveX, saveY, 54, 16, 0x44CC44);
        if (mx >= cancelX && mx < cancelX + 54 && my >= saveY && my < saveY + 16)
            renderGlow(g, cancelX, saveY, 54, 16, 0xFF4444);
    }

    private void renderItemsSection(GuiGraphics g, int mx, int my) {
        // ── Fond des boutons de mode ────────────────────────────────────
        int modeW = (PANEL_W - 14) / 4;
        g.fill(px + 4,               py + 22, px + 4  + modeW,         py + 34,
               itemMode == ItemMode.USE     ? 0xAA4488FF : 0x22FFFFFF);
        g.fill(px + 6 + modeW,       py + 22, px + 6  + modeW * 2,     py + 34,
               itemMode == ItemMode.BREAK   ? 0xAAFF8800 : 0x22FFFFFF);
        g.fill(px + 8 + modeW * 2,   py + 22, px + 8  + modeW * 3,     py + 34,
               itemMode == ItemMode.CRAFT   ? 0xAA44BB44 : 0x22FFFFFF);
        g.fill(px + 10 + modeW * 3,  py + 22, px + 10 + modeW * 4,     py + 34,
               itemMode == ItemMode.INTERACT ? 0xAA00BBCC : 0x22FFFFFF);

        // ── Onglet actif ────────────────────────────────────────────────
        if (!activeMetier.isEmpty())
            g.drawString(this.font, "► " + activeMetier, px + 4, py - 12, 0xFFDD88, false);

        // ── Bande de filtre par mod ─────────────────────────────────────
        int modAreaX = px + 4;
        int modAreaW = PANEL_W - 12 - SORT_BTN_W;
        int modY     = py + MOD_Y_OFF;
        int sortX    = px + PANEL_W - 4 - SORT_BTN_W;

        g.fill(modAreaX, modY, modAreaX + modAreaW, modY + MOD_H, 0x22000000);

        g.enableScissor(modAreaX, modY, modAreaX + modAreaW, modY + MOD_H);
        int nx = modAreaX - modScrollX;
        nx = renderModBtn(g, "Tous", nx, modY, activeNamespace == null, mx, my);
        for (String ns : allNamespaces)
            nx = renderModBtn(g, ns, nx, modY, ns.equals(activeNamespace), mx, my);
        g.disableScissor();

        int totalW = totalModButtonsWidth();
        if (totalW > modAreaW) {
            float ratio = (float) modScrollX / Math.max(1, totalW - modAreaW);
            int barW  = Math.max(16, modAreaW * modAreaW / totalW);
            int barX  = modAreaX + (int) (ratio * (modAreaW - barW));
            g.fill(barX, modY + MOD_H - 1, barX + barW, modY + MOD_H, 0x88FFFFFF);
        }

        String sortLabel = sortMode == SortMode.ALPHA_ASC  ? "A→Z" :
                           sortMode == SortMode.ALPHA_DESC ? "Z→A" : "Niv↓";
        boolean hovSort = mx >= sortX && mx < sortX + SORT_BTN_W
                       && my >= modY && my < modY + MOD_H;
        g.fill(sortX, modY, sortX + SORT_BTN_W, modY + MOD_H,
               hovSort ? 0xAA88AAFF : 0x6688AAFF);
        g.drawString(this.font, sortLabel, sortX + 4, modY + 2, 0xFFFFFF, false);

        // ── Grille d'items ──────────────────────────────────────────────
        int gridX    = px + (PANEL_W - COL * CELL) / 2;
        int gridY    = py + GRID_Y_OFF;
        int startIdx = itemScroll * COL;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COL; col++) {
                int idx = startIdx + row * COL + col;
                if (idx >= filteredItems.size()) break;
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(filteredItems.get(idx));
                int gx = gridX + col * CELL, gy = gridY + row * CELL;
                g.fill(gx, gy, gx + CELL - 1, gy + CELL - 1,
                       getLevel(itemId) > 0 ? 0x55FF8800 : 0x22FFFFFF);
                g.renderItem(new ItemStack(filteredItems.get(idx)), gx + 1, gy + 1);
            }
        }

        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COL; col++) {
                int idx = startIdx + row * COL + col;
                if (idx >= filteredItems.size()) break;
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(filteredItems.get(idx));
                int level = getLevel(itemId);
                if (level > 0)
                    g.drawString(this.font, String.valueOf(level),
                        gridX + col * CELL + 11, gridY + row * CELL + 9, 0xFFFF55, true);
            }
        }
        g.pose().popPose();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COL; col++) {
                int idx = startIdx + row * COL + col;
                if (idx >= filteredItems.size()) break;
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(filteredItems.get(idx));
                int gx = gridX + col * CELL, gy = gridY + row * CELL;
                if (mx >= gx && mx < gx + CELL && my >= gy && my < gy + CELL) {
                    int level  = getLevel(itemId);
                    int maxLvl = editMaxLevels.getOrDefault(activeMetier, 5);
                    String tip = itemId + (level > 0 ? "  [niv. " + level + "/" + maxLvl + "]" : "  [libre]");
                    g.renderTooltip(this.font, Component.literal(tip), mx, my);
                }
            }
        }

        int totalRows = (filteredItems.size() + COL - 1) / COL;
        int maxScroll = Math.max(0, totalRows - ROWS);
        if (maxScroll > 0)
            g.drawString(this.font, "▲▼ " + (itemScroll + 1) + "/" + (maxScroll + 1),
                px + PANEL_W - 52, py + PANEL_H - 46, 0xAAAAAA, false);
        g.drawString(this.font, "Clic G : +niv  |  Clic D : -niv  |  0 = libre",
            px + 4, py + PANEL_H - 46, 0x666666, false);
    }

    /** Dessine un bouton mod et retourne le prochain x. */
    private int renderModBtn(GuiGraphics g, String label, int bx, int modY,
                              boolean active, int mx, int my) {
        int bw  = this.font.width(label) + 6;
        int col = active       ? 0xBB6699CC
                : (mx >= bx && mx < bx + bw && my >= modY && my < modY + MOD_H)
                               ? 0x7755AAEE
                               : 0x44AAAAAA;
        g.fill(bx, modY, bx + bw, modY + MOD_H, col);
        g.drawString(this.font, label, bx + 3, modY + 2, active ? 0xFFFFFF : 0xBBBBBB, false);
        return bx + bw + 2;
    }

    private void renderNiveauxSection(GuiGraphics g) {
        g.drawString(this.font, "Niveau maximum par métier :", px + 8, py + 12, 0xAAAAAA, false);
        int y0 = py + 24, rowH = 22, shown = 0;
        for (int i = niveauxScroll; i < tabs.size(); i++) {
            String tab = tabs.get(i);
            int y = y0 + (i - niveauxScroll) * rowH;
            if (y + 16 > py + PANEL_H - 25) break;
            shown++;
            g.drawString(this.font, tab, px + 8, y + 4, 0xFFFFFF, false);
            g.drawString(this.font, "max : " + editMaxLevels.getOrDefault(tab, 5), px + 155, y + 4, 0xFFDD88, false);
        }
        if (niveauxScroll > 0 || niveauxScroll + shown < tabs.size())
            g.drawString(this.font, "▲▼  " + (niveauxScroll + 1) + "-" + (niveauxScroll + shown) + "/" + tabs.size(),
                px + 240, py + PANEL_H - 44, 0x888888, false);
    }

    private void renderMaitriseSection(GuiGraphics g) {
        g.drawString(this.font, "Spécialisations disponibles au niveau max :", px + 8, py + 12, 0xAAAAAA, false);
        int y0 = py + 24, rowH = 40, shown = 0;
        for (int i = maitriseScroll; i < tabs.size(); i++) {
            String tab = tabs.get(i);
            int y = y0 + (i - maitriseScroll) * rowH;
            if (y + 32 > py + PANEL_H - 25) break;
            shown++;
            g.drawString(this.font, tab, px + 8, y + 4, 0xFFFFFF, false);
            g.drawString(this.font, "Spé 1 →", px + 64, y + 4, 0x888888, false);
            g.drawString(this.font, "Spé 2 →", px + 64, y + 24, 0x888888, false);
        }
        if (maitriseScroll > 0 || maitriseScroll + shown < tabs.size())
            g.drawString(this.font, "▲▼  " + (maitriseScroll + 1) + "-" + (maitriseScroll + shown) + "/" + tabs.size(),
                px + 240, py + PANEL_H - 44, 0x888888, false);
    }

    private void renderElevageSection(GuiGraphics g) {
        g.drawString(this.font, "Reproduction par métier / niveau :", px + 8, py + 12, 0xAAAAAA, false);
        if (!activeMetier.isEmpty())
            g.drawString(this.font, "► " + activeMetier, px + 4, py - 12, 0xFFDD88, false);

        int y0 = py + 24, rowH = 20, shown = 0;
        for (int i = breedScroll; i < BREED_ENTITIES.length; i++) {
            int y = y0 + (i - breedScroll) * rowH;
            if (y + 14 > py + PANEL_H - 25) break;
            shown++;
            int level = getBreedLevel(BREED_ENTITIES[i]);
            g.drawString(this.font, BREED_NAMES[i], px + 8, y + 3, 0xFFFFFF, false);
            String lvlStr = level == 0 ? "Libre" : "Niv. " + level;
            int lvlColor  = level == 0 ? 0x888888 : 0xFFDD88;
            g.drawString(this.font, lvlStr, px + 240, y + 3, lvlColor, false);
        }
        if (breedScroll > 0 || breedScroll + shown < BREED_ENTITIES.length)
            g.drawString(this.font,
                "▲▼  " + (breedScroll + 1) + "-" + (breedScroll + shown) + "/" + BREED_ENTITIES.length,
                px + 240, py + PANEL_H - 44, 0x888888, false);
    }

    private void renderPotionsSection(GuiGraphics g) {
        g.drawString(this.font, "Potions par métier / niveau :", px + 8, py + 12, 0xAAAAAA, false);
        if (!activeMetier.isEmpty())
            g.drawString(this.font, "► " + activeMetier, px + 4, py - 12, 0xFFDD88, false);

        int y0 = py + 24, rowH = 20, shown = 0;
        for (int i = potionScroll; i < POTION_IDS.length; i++) {
            int y = y0 + (i - potionScroll) * rowH;
            if (y + 14 > py + PANEL_H - 25) break;
            shown++;
            int level = getPotionLevel(POTION_IDS[i]);
            g.drawString(this.font, POTION_NAMES[i], px + 8, y + 3, 0xFFFFFF, false);
            String lvlStr = level == 0 ? "Libre" : "Niv. " + level;
            int lvlColor  = level == 0 ? 0x888888 : 0xAA55FF;
            g.drawString(this.font, lvlStr, px + 240, y + 3, lvlColor, false);
        }
        if (potionScroll > 0 || potionScroll + shown < POTION_IDS.length)
            g.drawString(this.font,
                "▲▼  " + (potionScroll + 1) + "-" + (potionScroll + shown) + "/" + POTION_IDS.length,
                px + 240, py + PANEL_H - 44, 0x888888, false);
    }

    private void renderPecheSection(GuiGraphics g) {
        g.drawString(this.font, "Pêche — niveau minimum par métier :", px + 8, py + 12, 0xAAAAAA, false);
        int y0 = py + 24, rowH = 20, shown = 0;
        for (int i = fishingScroll; i < tabs.size(); i++) {
            String tab = tabs.get(i);
            int y = y0 + (i - fishingScroll) * rowH;
            if (y + 14 > py + PANEL_H - 25) break;
            shown++;
            int level = editFishing.getOrDefault(tab, 0);
            g.drawString(this.font, tab, px + 8, y + 3, 0xFFFFFF, false);
            String lvlStr = level == 0 ? "Libre" : "Niv. " + level;
            int lvlColor  = level == 0 ? 0x888888 : 0x29B6F6;
            g.drawString(this.font, lvlStr, px + 240, y + 3, lvlColor, false);
        }
        if (fishingScroll > 0 || fishingScroll + shown < tabs.size())
            g.drawString(this.font,
                "▲▼  " + (fishingScroll + 1) + "-" + (fishingScroll + shown) + "/" + tabs.size(),
                px + 240, py + PANEL_H - 44, 0x888888, false);
    }

    private void renderChasseSection(GuiGraphics g, int mx, int my) {
        g.drawString(this.font, "Chasse — niveau min. CHASSEUR (tous mods) :", px + 8, py + 12, 0xAAAAAA, false);

        // ── Bande de filtre par namespace de mod ───────────────────────
        int nsY     = py + 38;
        int nsAreaX = px + 4;
        int nsAreaW = PANEL_W - 8;
        g.fill(nsAreaX, nsY, nsAreaX + nsAreaW, nsY + MOD_H, 0x22000000);
        g.enableScissor(nsAreaX, nsY, nsAreaX + nsAreaW, nsY + MOD_H);
        int nx = nsAreaX - chasseModScrollX;
        nx = renderModBtn(g, "Tous", nx, nsY, chasseNamespace == null, mx, my);
        for (String ns : chasseNamespaces)
            nx = renderModBtn(g, ns, nx, nsY, ns.equals(chasseNamespace), mx, my);
        g.disableScissor();

        int totalNsW = totalChasseNsWidth();
        if (totalNsW > nsAreaW) {
            float ratio = (float) chasseModScrollX / Math.max(1, totalNsW - nsAreaW);
            int barW = Math.max(16, nsAreaW * nsAreaW / totalNsW);
            int barX = nsAreaX + (int) (ratio * (nsAreaW - barW));
            g.fill(barX, nsY + MOD_H - 1, barX + barW, nsY + MOD_H, 0x88FFFFFF);
        }

        // ── Liste des entités ──────────────────────────────────────────
        int y0 = py + 52, rowH = 18, shown = 0;
        for (int i = chasseScroll; i < filteredEntityIds.size(); i++) {
            ResourceLocation eid = filteredEntityIds.get(i);
            int y = y0 + (i - chasseScroll) * rowH;
            if (y + rowH > py + PANEL_H - 50) break;
            shown++;

            int level = editHunting.getOrDefault(eid, 0);

            // Namespace (gris) + path (blanc)
            String ns   = eid.getNamespace() + ":";
            String path = eid.getPath().replace("_", " ");
            int nsW = this.font.width(ns);
            g.drawString(this.font, ns,   px + 8,      y + 4, 0x888888, false);
            g.drawString(this.font, path, px + 8 + nsW + 1, y + 4, 0xFFFFFF, false);

            // Niveau requis
            String lvlStr = level == 0 ? "Libre" : "Niv. " + level;
            int lvlColor  = level == 0 ? 0x888888 : 0x4CAF50; // vert Chasseur
            g.drawString(this.font, lvlStr, px + 240, y + 4, lvlColor, false);

            // Tooltip si survol du nom
            if (mx >= px + 8 && mx < px + 240 && my >= y && my < y + rowH)
                g.renderTooltip(this.font, Component.literal(eid.toString()
                    + (level > 0 ? "  [niv. " + level + "]" : "  [libre]")), mx, my);
        }

        // ── Indicateurs ────────────────────────────────────────────────
        if (shown > 0 && (chasseScroll > 0 || chasseScroll + shown < filteredEntityIds.size()))
            g.drawString(this.font,
                "▲▼ " + (chasseScroll + 1) + "-" + (chasseScroll + shown) + "/" + filteredEntityIds.size(),
                px + PANEL_W - 80, py + PANEL_H - 44, 0x888888, false);
        if (filteredEntityIds.isEmpty())
            g.drawString(this.font, "Aucun mob trouvé.", px + 8, py + 60, 0x888888, false);
    }

    private void renderForgeSection(GuiGraphics g, int mx, int my) {
        g.drawString(this.font, "Forge — niveau min. FORGERON pour fondre (Tinkers') :", px + 8, py + 12, 0xB0B0B0, false);

        // ── Bande de filtre par namespace ──────────────────────────────
        int nsY     = py + 38;
        int nsAreaX = px + 4;
        int nsAreaW = PANEL_W - 8;
        g.fill(nsAreaX, nsY, nsAreaX + nsAreaW, nsY + MOD_H, 0x22000000);
        g.enableScissor(nsAreaX, nsY, nsAreaX + nsAreaW, nsY + MOD_H);
        int nx = nsAreaX - forgeModScrollX;
        nx = renderModBtn(g, "Tous", nx, nsY, forgeNamespace == null, mx, my);
        for (String ns : forgeNamespaces)
            nx = renderModBtn(g, ns, nx, nsY, ns.equals(forgeNamespace), mx, my);
        g.disableScissor();

        int totalNsW = totalForgeNsWidth();
        if (totalNsW > nsAreaW) {
            float ratio = (float) forgeModScrollX / Math.max(1, totalNsW - nsAreaW);
            int barW = Math.max(16, nsAreaW * nsAreaW / totalNsW);
            int barX = nsAreaX + (int) (ratio * (nsAreaW - barW));
            g.fill(barX, nsY + MOD_H - 1, barX + barW, nsY + MOD_H, 0x88FFFFFF);
        }

        // ── Liste des items ────────────────────────────────────────────
        int y0 = py + 52, rowH = 18, shown = 0;
        for (int i = forgeScroll; i < filteredSmeltIds.size(); i++) {
            ResourceLocation iid = filteredSmeltIds.get(i);
            int y = y0 + (i - forgeScroll) * rowH;
            if (y + rowH > py + PANEL_H - 50) break;
            shown++;

            int level = editSmelt.getOrDefault(iid, 0);
            String ns   = iid.getNamespace() + ":";
            String path = iid.getPath().replace("_", " ");
            int nsW = this.font.width(ns);
            g.drawString(this.font, ns,   px + 8,          y + 4, 0x888888, false);
            g.drawString(this.font, path, px + 8 + nsW + 1, y + 4, 0xFFFFFF, false);

            String lvlStr = level == 0 ? "Libre" : "Niv. " + level;
            int lvlColor  = level == 0 ? 0x888888 : 0xB0B0B0;
            g.drawString(this.font, lvlStr, px + 240, y + 4, lvlColor, false);

            if (mx >= px + 8 && mx < px + 240 && my >= y && my < y + rowH)
                g.renderTooltip(this.font, Component.literal(iid.toString()
                    + (level > 0 ? "  [FORGERON niv. " + level + "]" : "  [libre]")), mx, my);
        }

        if (shown > 0 && (forgeScroll > 0 || forgeScroll + shown < filteredSmeltIds.size()))
            g.drawString(this.font,
                "▲▼ " + (forgeScroll + 1) + "-" + (forgeScroll + shown) + "/" + filteredSmeltIds.size(),
                px + PANEL_W - 80, py + PANEL_H - 44, 0x888888, false);

        if (filteredSmeltIds.isEmpty())
            g.drawString(this.font, "Aucun item trouvé.", px + 8, py + 60, 0x888888, false);
    }

    private void renderCouleursSection(GuiGraphics g) {
        g.drawString(this.font, "Couleur de chat par métier :", px + 8, py + 12, 0xAAAAAA, false);
        g.drawString(this.font, "Code hex (ex: F5A623)", px + 220, py + 12, 0x666666, false);

        int y0 = py + 24, rowH = 22, shown = 0;
        for (int i = colorScroll; i < tabs.size(); i++) {
            String tab = tabs.get(i);
            int y = y0 + (i - colorScroll) * rowH;
            if (y + 16 > py + PANEL_H - 25) break;
            shown++;
            int color = editChatColors.getOrDefault(tab, MetierLevelConfig.getChatColor(tab));
            g.drawString(this.font, tab, px + 8, y + 4, 0xFF000000 | color, false);
            g.fill(px + 200, y + 1, px + 216, y + 15, 0xFF000000 | (color & 0xFFFFFF));
            g.renderOutline(px + 200, y + 1, 16, 14, 0xFF888888);
        }
        if (colorScroll > 0 || colorScroll + shown < tabs.size())
            g.drawString(this.font,
                "▲▼  " + (colorScroll + 1) + "-" + (colorScroll + shown) + "/" + tabs.size(),
                px + 240, py + PANEL_H - 44, 0x888888, false);
    }

    // ── Cycles ─────────────────────────────────────────────────────────────

    private void cycleFishingLevel(String metier, int dir) {
        int maxLvl = editMaxLevels.getOrDefault(metier, 5);
        int next = Math.max(0, Math.min(maxLvl, editFishing.getOrDefault(metier, 0) + dir));
        if (next == 0) editFishing.remove(metier);
        else           editFishing.put(metier, next);
    }

    private void cycleHuntingLevel(ResourceLocation entityId, int dir) {
        int maxLvl = editMaxLevels.getOrDefault("CHASSEUR", 5);
        int next = Math.max(0, Math.min(maxLvl, editHunting.getOrDefault(entityId, 0) + dir));
        if (next == 0) editHunting.remove(entityId);
        else           editHunting.put(entityId, next);
    }

    private int getPotionLevel(ResourceLocation potionId) {
        if (activeMetier.isEmpty()) return 0;
        return editPotions.getOrDefault(activeMetier, Collections.emptyMap())
                          .getOrDefault(potionId, 0);
    }

    private void cyclePotionLevel(ResourceLocation potionId, int dir) {
        if (activeMetier.isEmpty()) return;
        int maxLvl = editMaxLevels.getOrDefault(activeMetier, 5);
        int next = Math.max(0, Math.min(maxLvl, getPotionLevel(potionId) + dir));
        editPotions.computeIfAbsent(activeMetier, k -> new HashMap<>());
        if (next == 0) editPotions.get(activeMetier).remove(potionId);
        else           editPotions.get(activeMetier).put(potionId, next);
    }

    private int getBreedLevel(ResourceLocation entityId) {
        if (activeMetier.isEmpty()) return 0;
        return editBreeds.getOrDefault(activeMetier, Collections.emptyMap())
                         .getOrDefault(entityId, 0);
    }

    private void cycleBreedLevel(ResourceLocation entityId, int dir) {
        if (activeMetier.isEmpty()) return;
        int maxLvl = editMaxLevels.getOrDefault(activeMetier, 5);
        int next = Math.max(0, Math.min(maxLvl, getBreedLevel(entityId) + dir));
        editBreeds.computeIfAbsent(activeMetier, k -> new HashMap<>());
        if (next == 0) editBreeds.get(activeMetier).remove(entityId);
        else           editBreeds.get(activeMetier).put(entityId, next);
    }

    // ── Saisie ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (activeSection == Section.ITEMS) {
            int modAreaX = px + 4;
            int modAreaW = PANEL_W - 12 - SORT_BTN_W;
            int modY     = py + MOD_Y_OFF;
            int sortX    = px + PANEL_W - 4 - SORT_BTN_W;

            if (my >= modY && my < modY + MOD_H) {
                if (mx >= sortX && mx < sortX + SORT_BTN_W) {
                    cycleSortMode();
                    return true;
                }
                if (mx >= modAreaX && mx < modAreaX + modAreaW) {
                    int x = modAreaX - modScrollX;
                    int bw = this.font.width("Tous") + 6;
                    if (mx >= x && mx < x + bw) {
                        activeNamespace = null; itemScroll = 0; refreshFilter(); return true;
                    }
                    x += bw + 2;
                    for (String ns : allNamespaces) {
                        bw = this.font.width(ns) + 6;
                        if (mx >= x && mx < x + bw) {
                            activeNamespace = ns.equals(activeNamespace) ? null : ns;
                            itemScroll = 0; refreshFilter(); return true;
                        }
                        x += bw + 2;
                    }
                }
                return super.mouseClicked(mx, my, btn);
            }

            int gridX    = px + (PANEL_W - COL * CELL) / 2;
            int gridY    = py + GRID_Y_OFF;
            int startIdx = itemScroll * COL;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COL; col++) {
                    int idx = startIdx + row * COL + col;
                    if (idx >= filteredItems.size()) break;
                    int gx = gridX + col * CELL, gy = gridY + row * CELL;
                    if (mx >= gx && mx < gx + CELL && my >= gy && my < gy + CELL) {
                        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(filteredItems.get(idx));
                        int maxLvl = editMaxLevels.getOrDefault(activeMetier, 5);
                        if (btn == 0) cycleLevel(itemId, +1, maxLvl);
                        else if (btn == 1) cycleLevel(itemId, -1, maxLvl);
                        return true;
                    }
                }
            }

        } else if (activeSection == Section.CHASSE) {
            // Clic sur le filtre namespace
            int nsY     = py + 38;
            int nsAreaX = px + 4;
            if (my >= nsY && my < nsY + MOD_H) {
                int x = nsAreaX - chasseModScrollX;
                int bw = this.font.width("Tous") + 6;
                if (mx >= x && mx < x + bw) {
                    chasseNamespace = null; chasseScroll = 0;
                    refreshEntityFilter(); rebuildSectionWidgets(); return true;
                }
                x += bw + 2;
                for (String ns : chasseNamespaces) {
                    bw = this.font.width(ns) + 6;
                    if (mx >= x && mx < x + bw) {
                        chasseNamespace = ns.equals(chasseNamespace) ? null : ns;
                        chasseScroll = 0;
                        refreshEntityFilter(); rebuildSectionWidgets(); return true;
                    }
                    x += bw + 2;
                }
                return true;
            }
        } else if (activeSection == Section.FORGE) {
            // Clic sur le filtre namespace
            int nsY     = py + 38;
            int nsAreaX = px + 4;
            if (my >= nsY && my < nsY + MOD_H) {
                int x = nsAreaX - forgeModScrollX;
                int bw = this.font.width("Tous") + 6;
                if (mx >= x && mx < x + bw) {
                    forgeNamespace = null; forgeScroll = 0;
                    refreshSmeltFilter(); rebuildSectionWidgets(); return true;
                }
                x += bw + 2;
                for (String ns : forgeNamespaces) {
                    bw = this.font.width(ns) + 6;
                    if (mx >= x && mx < x + bw) {
                        forgeNamespace = ns.equals(forgeNamespace) ? null : ns;
                        forgeScroll = 0;
                        refreshSmeltFilter(); rebuildSectionWidgets(); return true;
                    }
                    x += bw + 2;
                }
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (activeSection == Section.ITEMS) {
            int modY = py + MOD_Y_OFF;
            if (my >= modY && my < modY + MOD_H) {
                int modAreaW  = PANEL_W - 12 - SORT_BTN_W;
                int maxScroll = Math.max(0, totalModButtonsWidth() - modAreaW);
                modScrollX = (int) Math.max(0, Math.min(maxScroll, modScrollX - delta * 14));
                return true;
            }
            int totalRows = (filteredItems.size() + COL - 1) / COL;
            int maxScroll = Math.max(0, totalRows - ROWS);
            itemScroll = (int) Math.max(0, Math.min(maxScroll, itemScroll - delta));
        } else if (activeSection == Section.NIVEAUX) {
            niveauxScroll = (int) Math.max(0, Math.min(tabs.size() - 1, niveauxScroll - delta));
            rebuildSectionWidgets();
        } else if (activeSection == Section.MAITRISE) {
            maitriseScroll = (int) Math.max(0, Math.min(tabs.size() - 1, maitriseScroll - delta));
            rebuildSectionWidgets();
        } else if (activeSection == Section.ELEVAGE) {
            breedScroll = (int) Math.max(0, Math.min(BREED_ENTITIES.length - 1, breedScroll - delta));
            rebuildSectionWidgets();
        } else if (activeSection == Section.POTIONS) {
            potionScroll = (int) Math.max(0, Math.min(POTION_IDS.length - 1, potionScroll - delta));
            rebuildSectionWidgets();
        } else if (activeSection == Section.PECHE) {
            fishingScroll = (int) Math.max(0, Math.min(tabs.size() - 1, fishingScroll - delta));
            rebuildSectionWidgets();
        } else if (activeSection == Section.CHASSE) {
            int nsY = py + 38;
            if (my >= nsY && my < nsY + MOD_H) {
                // Scroll horizontal de la bande namespace
                int nsAreaW   = PANEL_W - 8;
                int maxNsScroll = Math.max(0, totalChasseNsWidth() - nsAreaW);
                chasseModScrollX = (int) Math.max(0, Math.min(maxNsScroll, chasseModScrollX - delta * 14));
                return true;
            }
            // Scroll vertical de la liste
            int rowsVisible = (py + PANEL_H - 50 - (py + 52)) / 18;
            int maxScroll   = Math.max(0, filteredEntityIds.size() - rowsVisible);
            chasseScroll = (int) Math.max(0, Math.min(maxScroll, chasseScroll - delta));
            rebuildSectionWidgets();
        } else if (activeSection == Section.FORGE) {
            int nsY = py + 38;
            if (my >= nsY && my < nsY + MOD_H) {
                int nsAreaW = PANEL_W - 8;
                int maxNsScroll = Math.max(0, totalForgeNsWidth() - nsAreaW);
                forgeModScrollX = (int) Math.max(0, Math.min(maxNsScroll, forgeModScrollX - delta * 14));
                return true;
            }
            int rowsVisible = (py + PANEL_H - 50 - (py + 52)) / 18;
            int maxScroll   = Math.max(0, filteredSmeltIds.size() - rowsVisible);
            forgeScroll = (int) Math.max(0, Math.min(maxScroll, forgeScroll - delta));
            rebuildSectionWidgets();
        } else if (activeSection == Section.COULEURS) {
            colorScroll = (int) Math.max(0, Math.min(tabs.size() - 1, colorScroll - delta));
            rebuildSectionWidgets();
        }
        return true;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void refreshFilter() {
        String q = searchBox != null ? searchBox.getValue().toLowerCase() : "";
        List<Item> source = (itemMode == ItemMode.BREAK || itemMode == ItemMode.INTERACT)
            ? allBlockItems : allItems;
        filteredItems = source.stream()
            .filter(item -> {
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
                if (key == null) return false;
                if (activeNamespace != null && !key.getNamespace().equals(activeNamespace)) return false;
                return key.toString().contains(q);
            })
            .sorted(getSortComparator())
            .collect(Collectors.toList());
    }

    private void refreshEntityFilter() {
        String q = chasseSearchQuery.toLowerCase();
        filteredEntityIds = allEntityIds.stream()
            .filter(id -> {
                if (chasseNamespace != null && !id.getNamespace().equals(chasseNamespace)) return false;
                return id.toString().contains(q);
            })
            .collect(Collectors.toList());
    }

    private int getLevel(ResourceLocation id) {
        if (id == null || activeMetier.isEmpty()) return 0;
        Map<String, Map<ResourceLocation, Integer>> src =
            itemMode == ItemMode.USE     ? editItems        :
            itemMode == ItemMode.BREAK   ? editBreaks       :
            itemMode == ItemMode.INTERACT ? editBlockInteract : editCraft;
        return src.getOrDefault(activeMetier, Collections.emptyMap()).getOrDefault(id, 0);
    }

    private void cycleLevel(ResourceLocation id, int dir, int maxLvl) {
        if (id == null || activeMetier.isEmpty()) return;
        Map<String, Map<ResourceLocation, Integer>> src =
            itemMode == ItemMode.USE     ? editItems        :
            itemMode == ItemMode.BREAK   ? editBreaks       :
            itemMode == ItemMode.INTERACT ? editBlockInteract : editCraft;
        int next = Math.max(0, Math.min(maxLvl, getLevel(id) + dir));
        src.computeIfAbsent(activeMetier, k -> new HashMap<>());
        if (next == 0) src.get(activeMetier).remove(id);
        else           src.get(activeMetier).put(id, next);
    }

    private void saveAndClose() {
        Map<String, List<String>> cleanMaitrise = new HashMap<>();
        editMaitrise.forEach((tab, opts) -> {
            List<String> filtered = opts.stream()
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
            if (!filtered.isEmpty()) cleanMaitrise.put(tab, filtered);
        });

        MetierLevelConfig.setAll(editItems);
        MetierLevelConfig.setAllBreakRestrictions(editBreaks);
        MetierLevelConfig.setAllCraftRestrictions(editCraft);
        MetierLevelConfig.setAllBreedRestrictions(editBreeds);
        MetierLevelConfig.setAllBlockInteractRestrictions(editBlockInteract);
        MetierLevelConfig.setAllPotionRestrictions(editPotions);
        MetierLevelConfig.setAllChatColors(editChatColors);
        MetierLevelConfig.setFishingRestrictions(editFishing);
        MetierLevelConfig.setHuntingRestrictions(editHunting);
        MetierLevelConfig.setSmeltRestrictions(editSmelt);
        MetierLevelConfig.setMaxLevels(editMaxLevels);
        MetierLevelConfig.setMaitrise(cleanMaitrise);
        MetierNetwork.CHANNEL.sendToServer(new SaveConfigPacket(MetierLevelConfig.toJson()));
        this.onClose();
    }

    private static Map<String, Map<ResourceLocation, Integer>> deepCopy(
            Map<String, Map<ResourceLocation, Integer>> src) {
        Map<String, Map<ResourceLocation, Integer>> copy = new HashMap<>();
        src.forEach((k, v) -> copy.put(k, new HashMap<>(v)));
        return copy;
    }

    @Override public boolean isPauseScreen() { return false; }
}
