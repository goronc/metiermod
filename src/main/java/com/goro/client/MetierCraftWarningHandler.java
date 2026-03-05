package com.goro.client;

import com.goro.capability.IPlayerMetier;
import com.goro.capability.PlayerMetierProvider;
import com.goro.config.MetierLevelConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MetierCraftWarningHandler {

    @SubscribeEvent
    public static void onContainerRenderForeground(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        // Seulement les écrans de craft (table 3×3 et inventaire 2×2)
        if (!(screen instanceof CraftingScreen) && !(screen instanceof InventoryScreen)) return;

        // Slot 0 = résultat du craft dans CraftingMenu et InventoryMenu
        Slot resultSlot = screen.getMenu().getSlot(0);
        ItemStack result = resultSlot.getItem();
        if (result.isEmpty()) return;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(result.getItem());
        if (itemId == null) return;

        Map<String, Integer> restrictions = MetierLevelConfig.getCraftRestrictionsForItem(itemId);
        if (restrictions.isEmpty()) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        if (canCraft(player, restrictions)) return;

        // ── Dessin de l'avertissement ────────────────────────────────────
        int slotX = screen.getGuiLeft() + resultSlot.x;
        int slotY = screen.getGuiTop()  + resultSlot.y;

        GuiGraphics g = event.getGuiGraphics();

        // Overlay rouge semi-transparent sur le slot résultat
        g.fill(slotX, slotY, slotX + 16, slotY + 16, 0x99FF2222);

        // "!" blanc en haut à droite du slot
        g.drawString(Minecraft.getInstance().font, "!", slotX + 10, slotY + 1, 0xFFFFFF, true);

        // Tooltip si la souris survole le slot
        int mx = event.getMouseX();
        int my = event.getMouseY();
        if (mx >= slotX && mx < slotX + 16 && my >= slotY && my < slotY + 16) {
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            int lineH = 10;
            int boxW  = 185;
            int boxH  = (2 + restrictions.size()) * lineH + 4;
            int bx    = slotX + 18;
            int by    = slotY;
            int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            if (bx + boxW > sw) bx = slotX - boxW - 2;
            if (by + boxH > sh) by = sh - boxH - 2;
            g.fill(bx - 3, by - 3, bx + boxW + 3, by + boxH + 1, 0xCC000000);
            int cy = by;
            g.drawString(font, "Craft réservé :", bx, cy, 0xFF5555, false); cy += lineH;
            for (Map.Entry<String, Integer> entry : restrictions.entrySet()) {
                g.drawString(font, "• " + entry.getKey() + " niv. " + entry.getValue(), bx + 4, cy, 0xAAAAAA, false);
                cy += lineH;
            }
            g.drawString(font, "(matériaux seront perdus)", bx, cy, 0x666666, false);
        }
    }

    private static boolean canCraft(Player player, Map<String, Integer> restrictions) {
        boolean[] result = { false };
        player.getCapability(PlayerMetierProvider.METIER_CAP).ifPresent(cap -> {
            for (Map.Entry<String, Integer> entry : restrictions.entrySet()) {
                if (getPlayerLevelForMetier(cap, entry.getKey()) >= entry.getValue()) {
                    result[0] = true;
                    break;
                }
            }
        });
        return result[0];
    }

    private static int getPlayerLevelForMetier(IPlayerMetier cap, String metierName) {
        int level = 0;
        if (cap.getPrincipal().name().equals(metierName))
            level = Math.max(level, cap.getPrincipalLevel());
        if (cap.getSecondaire().name().equals(metierName))
            level = Math.max(level, cap.getSecondaireLevel());
        if (!cap.getMaitrise().isEmpty() && cap.getMaitrise().equals(metierName))
            level = Math.max(level, cap.getMaitriseLevel());
        if (!cap.getMaitrise().isEmpty()
                && MetierLevelConfig.getMaitriseOptions(metierName).contains(cap.getMaitrise()))
            level = Math.max(level, MetierLevelConfig.getMaxLevel(metierName));
        return level;
    }
}
