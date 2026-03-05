package com.goro.config;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Config unifiée :
 *  - itemRestrictions  : métierName → (itemId   → niveau minimum requis pour utiliser/tenir l'item)
 *  - breakRestrictions : métierName → (blockId  → niveau minimum requis pour casser le bloc)
 *  - maxLevels         : métierName → niveau maximum (défaut 5)
 *  - maitrise          : métierName → liste des métiers de spécialisation disponibles au niveau max
 *
 * Sauvegardée dans config/metiermod_config.json
 */
public class MetierLevelConfig {

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("metiermod_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, Map<ResourceLocation, Integer>> itemRestrictions         = new HashMap<>();
    private static Map<String, Map<ResourceLocation, Integer>> breakRestrictions        = new HashMap<>();
    private static Map<String, Map<ResourceLocation, Integer>> craftRestrictions        = new HashMap<>();
    private static Map<String, Map<ResourceLocation, Integer>> breedRestrictions        = new HashMap<>();
    private static Map<String, Map<ResourceLocation, Integer>> blockInteractRestrictions = new HashMap<>();
    private static Map<String, Map<ResourceLocation, Integer>> potionRestrictions        = new HashMap<>();
    private static Map<String, Integer>                        maxLevels                = new HashMap<>();
    private static Map<String, List<String>>                   maitrise                 = new HashMap<>();
    private static Map<String, Integer>                        chatColors               = new HashMap<>();
    private static Map<String, Integer>                        fishingRestrictions      = new HashMap<>();
    private static Map<ResourceLocation, Integer>              huntingRestrictions      = new HashMap<>();
    private static Map<ResourceLocation, Integer>              smeltRestrictions        = new HashMap<>();

    // ── Lecture / écriture ────────────────────────────────────────────────

    public static void load() {
        if (!Files.exists(FILE)) { save(); return; }
        try (Reader r = Files.newBufferedReader(FILE)) {
            fromJson(new com.google.gson.stream.JsonReader(r));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(buildJson(), w);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Sérialisation réseau ───────────────────────────────────────────────

    public static String toJson() {
        return GSON.toJson(buildJson());
    }

    public static void fromJson(String json) {
        try {
            parseRoot(JsonParser.parseString(json).getAsJsonObject());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Helpers privés ────────────────────────────────────────────────────

    private static JsonObject buildJson() {
        JsonObject root = new JsonObject();

        JsonObject items = new JsonObject();
        itemRestrictions.forEach((metier, map) -> {
            JsonObject obj = new JsonObject();
            map.forEach((id, lvl) -> obj.addProperty(id.toString(), lvl));
            items.add(metier, obj);
        });
        root.add("itemRestrictions", items);

        JsonObject breaks = new JsonObject();
        breakRestrictions.forEach((metier, map) -> {
            JsonObject obj = new JsonObject();
            map.forEach((id, lvl) -> obj.addProperty(id.toString(), lvl));
            breaks.add(metier, obj);
        });
        root.add("breakRestrictions", breaks);

        JsonObject crafts = new JsonObject();
        craftRestrictions.forEach((metier, map) -> {
            JsonObject obj = new JsonObject();
            map.forEach((id, lvl) -> obj.addProperty(id.toString(), lvl));
            crafts.add(metier, obj);
        });
        root.add("craftRestrictions", crafts);

        JsonObject breeds = new JsonObject();
        breedRestrictions.forEach((metier, map) -> {
            JsonObject obj = new JsonObject();
            map.forEach((id, lvl) -> obj.addProperty(id.toString(), lvl));
            breeds.add(metier, obj);
        });
        root.add("breedRestrictions", breeds);

        JsonObject blockInteracts = new JsonObject();
        blockInteractRestrictions.forEach((metier, map) -> {
            JsonObject obj = new JsonObject();
            map.forEach((id, lvl) -> obj.addProperty(id.toString(), lvl));
            blockInteracts.add(metier, obj);
        });
        root.add("blockInteractRestrictions", blockInteracts);

        JsonObject potions = new JsonObject();
        potionRestrictions.forEach((metier, map) -> {
            JsonObject obj = new JsonObject();
            map.forEach((id, lvl) -> obj.addProperty(id.toString(), lvl));
            potions.add(metier, obj);
        });
        root.add("potionRestrictions", potions);

        JsonObject maxLvls = new JsonObject();
        maxLevels.forEach(maxLvls::addProperty);
        root.add("maxLevels", maxLvls);

        JsonObject matr = new JsonObject();
        maitrise.forEach((metier, opts) -> {
            JsonArray arr = new JsonArray();
            opts.forEach(arr::add);
            matr.add(metier, arr);
        });
        root.add("maitrise", matr);

        JsonObject colors = new JsonObject();
        chatColors.forEach(colors::addProperty);
        root.add("chatColors", colors);

        JsonObject fishing = new JsonObject();
        fishingRestrictions.forEach(fishing::addProperty);
        root.add("fishingRestrictions", fishing);

        JsonObject hunting = new JsonObject();
        huntingRestrictions.forEach((id, lvl) -> hunting.addProperty(id.toString(), lvl));
        root.add("huntingRestrictions", hunting);

        JsonObject smelt = new JsonObject();
        smeltRestrictions.forEach((id, lvl) -> smelt.addProperty(id.toString(), lvl));
        root.add("smeltRestrictions", smelt);

        return root;
    }

    private static void parseRoot(JsonObject root) {
        itemRestrictions = new HashMap<>();
        if (root.has("itemRestrictions")) {
            for (Map.Entry<String, JsonElement> mEntry : root.get("itemRestrictions").getAsJsonObject().entrySet()) {
                Map<ResourceLocation, Integer> map = new HashMap<>();
                for (Map.Entry<String, JsonElement> iEntry : mEntry.getValue().getAsJsonObject().entrySet()) {
                    map.put(new ResourceLocation(iEntry.getKey()), iEntry.getValue().getAsInt());
                }
                itemRestrictions.put(mEntry.getKey(), map);
            }
        }

        breakRestrictions = new HashMap<>();
        if (root.has("breakRestrictions")) {
            for (Map.Entry<String, JsonElement> mEntry : root.get("breakRestrictions").getAsJsonObject().entrySet()) {
                Map<ResourceLocation, Integer> map = new HashMap<>();
                for (Map.Entry<String, JsonElement> iEntry : mEntry.getValue().getAsJsonObject().entrySet()) {
                    map.put(new ResourceLocation(iEntry.getKey()), iEntry.getValue().getAsInt());
                }
                breakRestrictions.put(mEntry.getKey(), map);
            }
        }

        craftRestrictions = new HashMap<>();
        if (root.has("craftRestrictions")) {
            for (Map.Entry<String, JsonElement> mEntry : root.get("craftRestrictions").getAsJsonObject().entrySet()) {
                Map<ResourceLocation, Integer> map = new HashMap<>();
                for (Map.Entry<String, JsonElement> iEntry : mEntry.getValue().getAsJsonObject().entrySet()) {
                    map.put(new ResourceLocation(iEntry.getKey()), iEntry.getValue().getAsInt());
                }
                craftRestrictions.put(mEntry.getKey(), map);
            }
        }

        breedRestrictions = new HashMap<>();
        if (root.has("breedRestrictions")) {
            for (Map.Entry<String, JsonElement> mEntry : root.get("breedRestrictions").getAsJsonObject().entrySet()) {
                Map<ResourceLocation, Integer> map = new HashMap<>();
                for (Map.Entry<String, JsonElement> iEntry : mEntry.getValue().getAsJsonObject().entrySet()) {
                    map.put(new ResourceLocation(iEntry.getKey()), iEntry.getValue().getAsInt());
                }
                breedRestrictions.put(mEntry.getKey(), map);
            }
        }

        blockInteractRestrictions = new HashMap<>();
        if (root.has("blockInteractRestrictions")) {
            for (Map.Entry<String, JsonElement> mEntry : root.get("blockInteractRestrictions").getAsJsonObject().entrySet()) {
                Map<ResourceLocation, Integer> map = new HashMap<>();
                for (Map.Entry<String, JsonElement> iEntry : mEntry.getValue().getAsJsonObject().entrySet()) {
                    map.put(new ResourceLocation(iEntry.getKey()), iEntry.getValue().getAsInt());
                }
                blockInteractRestrictions.put(mEntry.getKey(), map);
            }
        }

        potionRestrictions = new HashMap<>();
        if (root.has("potionRestrictions")) {
            for (Map.Entry<String, JsonElement> mEntry : root.get("potionRestrictions").getAsJsonObject().entrySet()) {
                Map<ResourceLocation, Integer> map = new HashMap<>();
                for (Map.Entry<String, JsonElement> iEntry : mEntry.getValue().getAsJsonObject().entrySet()) {
                    map.put(new ResourceLocation(iEntry.getKey()), iEntry.getValue().getAsInt());
                }
                potionRestrictions.put(mEntry.getKey(), map);
            }
        }

        maxLevels = new HashMap<>();
        if (root.has("maxLevels")) {
            for (Map.Entry<String, JsonElement> e : root.get("maxLevels").getAsJsonObject().entrySet()) {
                maxLevels.put(e.getKey(), e.getValue().getAsInt());
            }
        }

        maitrise = new HashMap<>();
        if (root.has("maitrise")) {
            for (Map.Entry<String, JsonElement> e : root.get("maitrise").getAsJsonObject().entrySet()) {
                List<String> opts = new ArrayList<>();
                JsonElement el = e.getValue();
                if (el.isJsonArray()) {
                    el.getAsJsonArray().forEach(s -> opts.add(s.getAsString()));
                } else if (el.isJsonPrimitive()) {
                    String s = el.getAsString();
                    if (!s.isEmpty()) opts.add(s); // rétrocompatibilité ancien format string
                }
                if (!opts.isEmpty()) maitrise.put(e.getKey(), opts);
            }
        }

        chatColors = new HashMap<>();
        if (root.has("chatColors")) {
            for (Map.Entry<String, JsonElement> e : root.get("chatColors").getAsJsonObject().entrySet()) {
                chatColors.put(e.getKey(), e.getValue().getAsInt());
            }
        }

        fishingRestrictions = new HashMap<>();
        if (root.has("fishingRestrictions")) {
            for (Map.Entry<String, JsonElement> e : root.get("fishingRestrictions").getAsJsonObject().entrySet()) {
                fishingRestrictions.put(e.getKey(), e.getValue().getAsInt());
            }
        }

        huntingRestrictions = new HashMap<>();
        if (root.has("huntingRestrictions")) {
            for (Map.Entry<String, JsonElement> e : root.get("huntingRestrictions").getAsJsonObject().entrySet()) {
                huntingRestrictions.put(new ResourceLocation(e.getKey()), e.getValue().getAsInt());
            }
        }

        smeltRestrictions = new HashMap<>();
        if (root.has("smeltRestrictions")) {
            for (Map.Entry<String, JsonElement> e : root.get("smeltRestrictions").getAsJsonObject().entrySet()) {
                smeltRestrictions.put(new ResourceLocation(e.getKey()), e.getValue().getAsInt());
            }
        }
    }

    private static void fromJson(com.google.gson.stream.JsonReader reader) throws Exception {
        parseRoot(JsonParser.parseReader(reader).getAsJsonObject());
    }

    // ── Accesseurs ────────────────────────────────────────────────────────

    /** Niveau max configuré pour un métier (défaut 5). */
    public static int getMaxLevel(String metierName) {
        return maxLevels.getOrDefault(metierName, 5);
    }

    /** Spécialisations disponibles quand ce métier atteint son niveau max (liste de 0 à N). */
    public static List<String> getMaitriseOptions(String metierName) {
        return maitrise.getOrDefault(metierName, Collections.emptyList());
    }

    /** Toutes les restrictions d'un item : métierName → niveau minimum (pour MetierComportements). */
    public static Map<String, Integer> getRestrictionsForItem(ResourceLocation itemId) {
        Map<String, Integer> result = new HashMap<>();
        itemRestrictions.forEach((metier, items) -> {
            if (items.containsKey(itemId)) result.put(metier, items.get(itemId));
        });
        return result;
    }

    /** Restrictions de casse pour un bloc : métierName → niveau minimum. */
    public static Map<String, Integer> getBreakRestrictionsForBlock(ResourceLocation blockId) {
        Map<String, Integer> result = new HashMap<>();
        breakRestrictions.forEach((metier, blocks) -> {
            if (blocks.containsKey(blockId)) result.put(metier, blocks.get(blockId));
        });
        return result;
    }

    /** Restrictions de craft pour un item : métierName → niveau minimum. */
    public static Map<String, Integer> getCraftRestrictionsForItem(ResourceLocation itemId) {
        Map<String, Integer> result = new HashMap<>();
        craftRestrictions.forEach((metier, items) -> {
            if (items.containsKey(itemId)) result.put(metier, items.get(itemId));
        });
        return result;
    }

    /** Restrictions de reproduction pour une entité : métierName → niveau minimum. */
    public static Map<String, Integer> getBreedRestrictionsForEntity(ResourceLocation entityId) {
        Map<String, Integer> result = new HashMap<>();
        breedRestrictions.forEach((metier, entities) -> {
            if (entities.containsKey(entityId)) result.put(metier, entities.get(entityId));
        });
        return result;
    }

    /** Restrictions d'interaction avec un bloc : métierName → niveau minimum. */
    public static Map<String, Integer> getBlockInteractRestrictionsForBlock(ResourceLocation blockId) {
        Map<String, Integer> result = new HashMap<>();
        blockInteractRestrictions.forEach((metier, blocks) -> {
            if (blocks.containsKey(blockId)) result.put(metier, blocks.get(blockId));
        });
        return result;
    }

    /** Restrictions d'utilisation d'une potion : métierName → niveau minimum. */
    public static Map<String, Integer> getPotionRestrictionsForPotion(ResourceLocation potionId) {
        Map<String, Integer> result = new HashMap<>();
        potionRestrictions.forEach((metier, potions) -> {
            if (potions.containsKey(potionId)) result.put(metier, potions.get(potionId));
        });
        return result;
    }

    public static Map<String, Map<ResourceLocation, Integer>> getAll()                          { return itemRestrictions;          }
    public static Map<String, Map<ResourceLocation, Integer>> getAllBreakRestrictions()          { return breakRestrictions;         }
    public static Map<String, Map<ResourceLocation, Integer>> getAllCraftRestrictions()          { return craftRestrictions;         }
    public static Map<String, Map<ResourceLocation, Integer>> getAllBreedRestrictions()          { return breedRestrictions;         }
    public static Map<String, Map<ResourceLocation, Integer>> getAllBlockInteractRestrictions()  { return blockInteractRestrictions; }
    public static Map<String, Map<ResourceLocation, Integer>> getAllPotionRestrictions()         { return potionRestrictions;        }
    public static Map<String, Integer>                        getMaxLevels()                    { return maxLevels;                 }
    public static Map<String, List<String>>                   getMaitrise()                     { return maitrise;                  }

    public static void setAll(Map<String, Map<ResourceLocation, Integer>> d)                         { itemRestrictions          = d; }
    public static void setAllBreakRestrictions(Map<String, Map<ResourceLocation, Integer>> d)         { breakRestrictions         = d; }
    public static void setAllCraftRestrictions(Map<String, Map<ResourceLocation, Integer>> d)         { craftRestrictions         = d; }
    public static void setAllBreedRestrictions(Map<String, Map<ResourceLocation, Integer>> d)         { breedRestrictions         = d; }
    public static void setAllBlockInteractRestrictions(Map<String, Map<ResourceLocation, Integer>> d) { blockInteractRestrictions = d; }
    public static void setAllPotionRestrictions(Map<String, Map<ResourceLocation, Integer>> d)        { potionRestrictions        = d; }
    public static void setMaxLevels(Map<String, Integer> m)                                          { maxLevels                 = m; }
    public static void setMaitrise(Map<String, List<String>> m)                                      { maitrise                  = m; }
    public static void setAllChatColors(Map<String, Integer> m)                                      { chatColors                = m; }

    public static Map<String, Integer>        getAllChatColors()        { return chatColors;          }
    public static Map<String, Integer>        getFishingRestrictions() { return fishingRestrictions; }
    public static void setFishingRestrictions(Map<String, Integer> m)  { fishingRestrictions = m;   }

    public static Map<ResourceLocation, Integer> getHuntingRestrictions() { return huntingRestrictions; }
    public static void setHuntingRestrictions(Map<ResourceLocation, Integer> m) { huntingRestrictions = m; }

    /** Niveau Chasseur minimum requis pour tuer cette entité (0 = aucune restriction). */
    public static int getHuntingLevelForEntity(ResourceLocation entityId) {
        return huntingRestrictions.getOrDefault(entityId, 0);
    }

    public static Map<ResourceLocation, Integer> getSmeltRestrictions() { return smeltRestrictions; }
    public static void setSmeltRestrictions(Map<ResourceLocation, Integer> m) { smeltRestrictions = m; }

    /** Niveau FORGERON minimum requis pour fondre cet item dans la forge (0 = aucune restriction). */
    public static int getSmeltLevelForItem(ResourceLocation itemId) {
        return smeltRestrictions.getOrDefault(itemId, 0);
    }

    /** Couleur de chat pour un métier (retourne la valeur config ou la couleur par défaut). */
    public static int getChatColor(String metierName) {
        return chatColors.getOrDefault(metierName, getDefaultChatColor(metierName));
    }

    private static int getDefaultChatColor(String name) {
        return switch (name) {
            case "CONSTRUCTEUR" -> 0xF5A623;
            case "FORGERON"     -> 0xB0B0B0;
            case "CHASSEUR"     -> 0x4CAF50;
            case "FERMIER"      -> 0xCDDC39;
            case "TAVERNIER"    -> 0xFF7043;
            case "APHOTICAIRE"  -> 0xCE93D8;
            case "PECHEUR"      -> 0x29B6F6;
            default             -> 0xAAAAAA;
        };
    }
}
