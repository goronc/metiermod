package com.goro.client;

import com.goro.config.MetierLevelConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Classe client uniquement — évite de charger MetierGuiScreen sur le serveur dédié. */
@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void openGui(String configJson) {
        MetierLevelConfig.fromJson(configJson);
        Minecraft.getInstance().setScreen(new MetierConfigScreen());
    }
}
