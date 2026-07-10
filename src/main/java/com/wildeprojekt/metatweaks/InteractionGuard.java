package com.wildeprojekt.metatweaks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.item.BoatItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Item;
import net.minecraft.item.MilkBucketItem;
import net.minecraft.item.MinecartItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class InteractionGuard {

    private static final Gson GSON = new Gson();
    private static final String BYPASS_PERMISSION = "metatweaks.bypass";
    private static final String CREATE_PERMISSION = "metatweaks.create";
    private static final String CONFIG_RESOURCE = "/metatweaks/block-blacklist.json";

    private static final Set<Identifier> ALLOWED_FLUID_BUCKETS = Set.of(
            new Identifier("minecraft:bucket"),
            new Identifier("minecraft:water_bucket"),
            new Identifier("minecraft:lava_bucket")
    );

    private static final Map<String, Predicate<Item>> GROUP_CHECKS = Map.of(
            "bucket", item -> item instanceof BucketItem
                    && !(item instanceof MilkBucketItem)
                    && !ALLOWED_FLUID_BUCKETS.contains(Registries.ITEM.getId(item)),
            "spawn_egg", item -> item instanceof SpawnEggItem,
            "minecart", item -> item instanceof MinecartItem,
            "trident", item -> item instanceof TridentItem,
            "boats", item -> item instanceof BoatItem,
            "egg", item -> item instanceof EggItem,
            "milk_bucket", item -> item instanceof MilkBucketItem,
            "throwable_potion", item -> item instanceof ThrowablePotionItem,
            "flint_and_steel", item -> item instanceof FlintAndSteelItem
    );

    private static Set<Identifier> blacklistedBlocks = Collections.emptySet();
    private static Set<Identifier> blacklistedItems = Collections.emptySet();
    private static Set<String> activeHardCodedGroups = Collections.emptySet();

    private InteractionGuard() {
    }

    static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("metatweaks");
        Path configFile = configDir.resolve("block-blacklist.json");

        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(configDir);
                try (InputStream defaultConfig = InteractionGuard.class.getResourceAsStream(CONFIG_RESOURCE)) {
                    if (defaultConfig == null) {
                        Log.warn(LogCategory.LOG, "MetaTweaks: default interaction config resource missing");
                        resetToEmpty();
                        return;
                    }
                    Files.copy(defaultConfig, configFile);
                    Log.info(LogCategory.LOG, "MetaTweaks: created default interaction config at " + configFile);
                }
            }

            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(configFile), StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) {
                    throw new JsonParseException("Root object is null");
                }

                JsonObject blacklist = root.has("blacklist") ? root.getAsJsonObject("blacklist") : root;
                blacklistedBlocks = parseIdSet(blacklist.getAsJsonArray("blocks"), "blacklist.blocks");
                blacklistedItems = parseIdSet(blacklist.getAsJsonArray("items"), "blacklist.items");
                activeHardCodedGroups = parseGroupSet(root.getAsJsonArray("hard-coded"));

                Log.info(LogCategory.LOG, "MetaTweaks: loaded interaction config — "
                        + blacklistedBlocks.size() + " blacklisted blocks, "
                        + blacklistedItems.size() + " blacklisted items, "
                        + activeHardCodedGroups.size() + " hard-coded groups");
            }
        } catch (IOException | JsonParseException e) {
            Log.error(LogCategory.LOG, "MetaTweaks: failed to load interaction config, using empty rules", e);
            resetToEmpty();
        }
    }

    private static void resetToEmpty() {
        blacklistedBlocks = Collections.emptySet();
        blacklistedItems = Collections.emptySet();
        activeHardCodedGroups = Collections.emptySet();
    }

    private static Set<Identifier> parseIdSet(JsonArray array, String fieldName) {
        Set<Identifier> ids = new HashSet<>();
        if (array == null) {
            return Collections.unmodifiableSet(ids);
        }
        for (var element : array) {
            try {
                ids.add(new Identifier(element.getAsString()));
            } catch (Exception e) {
                Log.warn(LogCategory.LOG, "MetaTweaks: skipping invalid " + fieldName + " entry: " + element);
            }
        }
        return Collections.unmodifiableSet(ids);
    }

    private static Set<String> parseGroupSet(JsonArray array) {
        Set<String> groups = new HashSet<>();
        if (array == null) {
            return Collections.unmodifiableSet(groups);
        }
        for (var element : array) {
            String group = element.getAsString();
            if (GROUP_CHECKS.containsKey(group)) {
                groups.add(group);
            } else {
                Log.warn(LogCategory.LOG, "MetaTweaks: skipping unknown hard-coded group: " + group);
            }
        }
        return Collections.unmodifiableSet(groups);
    }

    public static boolean isHardCodedRestricted(Item item) {
        for (String group : activeHardCodedGroups) {
            Predicate<Item> check = GROUP_CHECKS.get(group);
            if (check != null && check.test(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCreateNamespace(Identifier id) {
        return "create".equals(id.getNamespace());
    }

    public static boolean isBlockBlacklisted(Identifier id) {
        return blacklistedBlocks.contains(id);
    }

    public static boolean isItemBlacklisted(Identifier id) {
        return blacklistedItems.contains(id);
    }

    public static boolean canHoldItem(ServerPlayerEntity player, Identifier itemId) {
        return canUseItem(player, itemId);
    }

    public static boolean canUseItem(ServerPlayerEntity player, Identifier itemId) {
        Item item = Registries.ITEM.get(itemId);

        if (isHardCodedRestricted(item)) {
            return MetaTweaks.hasPermission(player, BYPASS_PERMISSION);
        }

        if (isCreateNamespace(itemId)) {
            if (!MetaTweaks.hasPermission(player, CREATE_PERMISSION)) {
                return false;
            }
            if (isItemBlacklisted(itemId) || isBlockBlacklisted(itemId)) {
                return MetaTweaks.hasPermission(player, BYPASS_PERMISSION);
            }
            return true;
        }

        if (isItemBlacklisted(itemId) || isBlockBlacklisted(itemId)) {
            return MetaTweaks.hasPermission(player, BYPASS_PERMISSION);
        }

        return true;
    }

    public static boolean canInteractBlock(ServerPlayerEntity player, Identifier blockId) {
        if (isCreateNamespace(blockId)) {
            if (!MetaTweaks.hasPermission(player, CREATE_PERMISSION)) {
                return false;
            }
            if (isBlockBlacklisted(blockId)) {
                return MetaTweaks.hasPermission(player, BYPASS_PERMISSION);
            }
            return true;
        }

        if (isBlockBlacklisted(blockId)) {
            return MetaTweaks.hasPermission(player, BYPASS_PERMISSION);
        }

        return true;
    }
}
