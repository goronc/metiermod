package com.goro.client;

import com.goro.config.MetierLevelConfig;
import com.goro.data.MetierPrincipal;
import com.goro.network.MetierNetwork;
import com.goro.network.SaveConfigPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class MetierGuiScreen extends Screen {

    // ── Dimensions ────────────────────────────────────────────────────
    private static final int PANEL_W    = 340;
    private static final int PANEL_H    = 220;
    private static final int COL        = 9;   // items par ligne
    private static final int CELL       = 18;  // taille d'une cellule
    private static final int ROWS       = 5;   // lignes visibles

    // ── État ──────────────────────────────────────────────────────────
    private final List<String>    tabs          = new ArrayList<>();
    private       String          activeTab     = "";
    private       EditBox         searchBox;
    private       List<Item>      allItems      = new ArrayList<>();
    private       List<Item>      filteredItems = new ArrayList<>();
    private       int             scroll        = 0;
    private       Map<String, Map<ResourceLocation, Integer>> editData;

    // ── Origine du panneau (calculée dans init) ───────────────────────
    private int px, py;

    public MetierGuiScreen() {
        super(Component.literal("Configuration des niveaux par Métier"));

        // Onglets dynamiques : un par métier (principal + secondaire, sans doublons)
        for (MetierPrincipal m : MetierPrincipal.values())
            if (m != MetierPrincipal.AUCUN) tabs.add(m.name());
        if (!tabs.contains("MINEUR"))   tabs.add("MINEUR");
        if (!tabs.contains("BUCHERON")) tabs.add("BUCHERON");

        activeTab = tabs.isEmpty() ? "" : tabs.get(0);
        editData  = deepCopy(MetierLevelConfig.getAll());
    }

    @Override
    protected void init() {
        super.init();

        int tabW       = 50;
        int tabH       = 14;
        int tabGap     = 2;
        int tabsPerRow = PANEL_W / (tabW + tabGap);
        int tabRows    = (tabs.size() + tabsPerRow - 1) / tabsPerRow;

        // Centrage vertical : panneau + rangées d'onglets au-dessus
        int totalH = PANEL_H + tabRows * (tabH + tabGap);
        px = (this.width  - PANEL_W) / 2;
        py = (this.height - totalH) / 2 + tabRows * (tabH + tabGap);

        // Boutons d'onglets
        for (int i = 0; i < tabs.size(); i++) {
            String tab  = tabs.get(i);
            int    col  = i % tabsPerRow;
            int    row  = i / tabsPerRow;
            int    bx   = px + col * (tabW + tabGap);
            int    by   = py - (tabRows - row) * (tabH + tabGap);
            this.addRenderableWidget(
                Button.builder(Component.literal(tab), btn -> {
                    activeTab = tab;
                    scroll    = 0;
                    refreshFilter();
                }).pos(bx, by).size(tabW, tabH).build()
            );
        }

        // Zone de recherche
        searchBox = new EditBox(this.font, px + 4, py + 4, PANEL_W - 8, 14, Component.literal(""));
        searchBox.setHint(Component.literal("Rechercher un item..."));
        searchBox.setResponder(s -> { scroll = 0; refreshFilter(); });
        this.addRenderableWidget(searchBox);

        // Boutons Sauvegarder / Annuler
        this.addRenderableWidget(Button.builder(
            Component.literal("Sauvegarder"), btn -> saveAndClose()
        ).pos(px + PANEL_W / 2 - 56, py + PANEL_H - 20).size(54, 16).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("Annuler"), btn -> this.onClose()
        ).pos(px + PANEL_W / 2 + 2, py + PANEL_H - 20).size(54, 16).build());

        // Liste de tous les items triée (construite une seule fois)
        allItems = ForgeRegistries.ITEMS.getValues().stream()
            .filter(item -> ForgeRegistries.ITEMS.getKey(item) != null)
            .sorted(Comparator.comparing(item -> ForgeRegistries.ITEMS.getKey(item).toString()))
            .collect(Collectors.toList());
        refreshFilter();
    }

    private void refreshFilter() {
        String q = searchBox != null ? searchBox.getValue().toLowerCase() : "";
        filteredItems = allItems.stream()
            .filter(item -> ForgeRegistries.ITEMS.getKey(item).toString().contains(q))
            .collect(Collectors.toList());
    }

    // ── Rendu ─────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);

        // Fond + bordure du panneau
        g.fill(px, py, px + PANEL_W, py + PANEL_H, 0xCC0D0D1F);
        g.renderOutline(px, py, PANEL_W, PANEL_H, 0xFF6060B0);

        // Onglet actif
        g.drawString(this.font, "► " + activeTab, px + 4, py - 12, 0xFFDD88, false);

        // Grille d'items
        int gridX     = px + (PANEL_W - COL * CELL) / 2;
        int gridY     = py + 22;
        int startIdx  = scroll * COL;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COL; col++) {
                int idx = startIdx + row * COL + col;
                if (idx >= filteredItems.size()) break;

                Item             item   = filteredItems.get(idx);
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                int x     = gridX + col * CELL;
                int y     = gridY + row * CELL;
                int level = getLevel(itemId);

                // Fond de cellule (orange si configuré, sinon blanc semi-transparent)
                g.fill(x, y, x + CELL - 1, y + CELL - 1, level > 0 ? 0x55FF8800 : 0x22FFFFFF);

                // Icône de l'item
                g.renderItem(new ItemStack(item), x + 1, y + 1);

                // Numéro de niveau (coin bas-droite)
                if (level > 0) {
                    g.drawString(this.font, String.valueOf(level), x + 11, y + 9, 0xFFFF55, true);
                }

                // Tooltip au survol
                if (mx >= x && mx < x + CELL && my >= y && my < y + CELL) {
                    String tip = itemId + (level > 0 ? "  [niv. min : " + level + "]" : "  [libre]");
                    g.renderTooltip(this.font, Component.literal(tip), mx, my);
                }
            }
        }

        // Indicateur de scroll
        int totalRows = (filteredItems.size() + COL - 1) / COL;
        int maxScroll = Math.max(0, totalRows - ROWS);
        if (maxScroll > 0) {
            g.drawString(this.font, "▲▼ " + (scroll + 1) + "/" + (maxScroll + 1),
                px + PANEL_W - 52, py + PANEL_H - 32, 0xAAAAAA, false);
        }

        // Légende
        g.drawString(this.font, "Clic G : +niv  |  Clic D : -niv  |  0 = libre",
            px + 4, py + PANEL_H - 32, 0x666666, false);

        super.render(g, mx, my, pt);
    }

    // ── Saisie ────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int gridX    = px + (PANEL_W - COL * CELL) / 2;
        int gridY    = py + 22;
        int startIdx = scroll * COL;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COL; col++) {
                int idx = startIdx + row * COL + col;
                if (idx >= filteredItems.size()) break;
                int x = gridX + col * CELL;
                int y = gridY + row * CELL;
                if (mx >= x && mx < x + CELL && my >= y && my < y + CELL) {
                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(filteredItems.get(idx));
                    if (btn == 0) cycleLevel(itemId, +1);
                    else if (btn == 1) cycleLevel(itemId, -1);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int totalRows = (filteredItems.size() + COL - 1) / COL;
        int maxScroll = Math.max(0, totalRows - ROWS);
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - delta));
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private int getLevel(ResourceLocation itemId) {
        if (itemId == null || activeTab.isEmpty()) return 0;
        return editData.getOrDefault(activeTab, Collections.emptyMap()).getOrDefault(itemId, 0);
    }

    private void cycleLevel(ResourceLocation itemId, int dir) {
        if (itemId == null || activeTab.isEmpty()) return;
        int next = Math.max(0, Math.min(5, getLevel(itemId) + dir));
        editData.computeIfAbsent(activeTab, k -> new HashMap<>());
        if (next == 0) editData.get(activeTab).remove(itemId);
        else           editData.get(activeTab).put(itemId, next);
    }

    private void saveAndClose() {
        MetierLevelConfig.setAll(editData);
        MetierNetwork.CHANNEL.sendToServer(new SaveConfigPacket(MetierLevelConfig.toJson()));
        this.onClose();
    }

    private static Map<String, Map<ResourceLocation, Integer>> deepCopy(
            Map<String, Map<ResourceLocation, Integer>> src) {
        Map<String, Map<ResourceLocation, Integer>> copy = new HashMap<>();
        src.forEach((k, v) -> copy.put(k, new HashMap<>(v)));
        return copy;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
