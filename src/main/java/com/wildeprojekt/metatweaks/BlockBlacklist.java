package com.wildeprojekt.metatweaks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
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
import java.util.Set;

final class BlockBlacklist {

    private static final Gson GSON = new Gson();
    private static final String BYPASS_PERMISSION = "metatweaks.bypass";
    private static final String CONFIG_RESOURCE = "/metatweaks/block-blacklist.json";

    private static Set<Identifier> blacklistedBlocks = Collections.emptySet();
    private static Set<Identifier> blacklistedItems = Collections.emptySet();

    private BlockBlacklist() {
    }

    static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("metatweaks");
        Path configFile = configDir.resolve("block-blacklist.json");

        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(configDir);
                try (InputStream defaultConfig = BlockBlacklist.class.getResourceAsStream(CONFIG_RESOURCE)) {
                    if (defaultConfig == null) {
                        Log.warn(LogCategory.LOG, "MetaTweaks: default block blacklist resource missing");
                        blacklistedBlocks = Collections.emptySet();
                        blacklistedItems = Collections.emptySet();
                        return;
                    }
                    Files.copy(defaultConfig, configFile);
                    Log.info(LogCategory.LOG, "MetaTweaks: created default block blacklist at " + configFile);
                }
            }

            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(configFile), StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) {
                    throw new JsonParseException("Root object is null");
                }
                blacklistedBlocks = parseIdSet(root.getAsJsonArray("blocks"), "blocks");
                blacklistedItems = parseIdSet(root.getAsJsonArray("items"), "items");
                Log.info(LogCategory.LOG, "MetaTweaks: loaded " + blacklistedBlocks.size()
                        + " blacklisted blocks and " + blacklistedItems.size() + " blacklisted items");
            }
        } catch (IOException | JsonParseException e) {
            Log.error(LogCategory.LOG, "MetaTweaks: failed to load block blacklist, using empty lists", e);
            blacklistedBlocks = Collections.emptySet();
            blacklistedItems = Collections.emptySet();
        }
    }

    private static Set<Identifier> parseIdSet(JsonArray array, String fieldName) {
        Set<Identifier> ids = new HashSet<>();
        if (array == null) {
            return ids;
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

    static boolean isBlockBlacklisted(Identifier id) {
        return blacklistedBlocks.contains(id);
    }

    static boolean isItemBlacklisted(Identifier id) {
        return blacklistedItems.contains(id);
    }

    static boolean isBlockInteractionAllowed(ServerPlayerEntity player, Identifier blockId) {
        if (!isBlockBlacklisted(blockId)) {
            return true;
        }
        return MetaTweaks.hasPermission(player, BYPASS_PERMISSION);
    }

    static boolean isItemUseAllowed(ServerPlayerEntity player, Identifier itemId) {
        if (!isItemBlacklisted(itemId) && !isBlockBlacklisted(itemId)) {
            return true;
        }
        return MetaTweaks.hasPermission(player, BYPASS_PERMISSION);
    }
}
