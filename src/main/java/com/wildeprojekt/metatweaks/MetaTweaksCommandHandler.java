package com.wildeprojekt.metatweaks;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

/**
 * Registers and implements commands for the MetaTweaks mod.
 * Commands:
 * - /metatweaks allowwaterspread: Toggles water spread (metatweaks.allowwaterspread or bypass).
 * - /metatweaks reload: Reloads block-blacklist.json (metatweaks.bypass).
 */
public class MetaTweaksCommandHandler {

    private static boolean hasBypassPermission(ServerCommandSource source) {
        if (source.getPlayer() instanceof ServerPlayerEntity) {
            return MetaTweaks.hasBypass((ServerPlayerEntity) source.getPlayer());
        }
        return source.hasPermissionLevel(2);
    }

    private static boolean hasAllowWaterSpreadPermission(ServerCommandSource source) {
        if (source.getPlayer() instanceof ServerPlayerEntity) {
            return MetaTweaks.hasAllowWaterSpread((ServerPlayerEntity) source.getPlayer());
        }
        return source.hasPermissionLevel(2);
    }

    /**
     * Registers all MetaTweaks commands on the provided dispatcher.
     */
    public static void MetaTweaksCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("metatweaks")
                .then(CommandManager.literal("allowwaterspread")
                        .requires(MetaTweaksCommandHandler::hasAllowWaterSpreadPermission)
                        .executes(context -> toggleWaterSpread(context.getSource())))
                .then(CommandManager.literal("reload")
                        .requires(MetaTweaksCommandHandler::hasBypassPermission)
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            if (!InteractionGuard.reload()) {
                                source.sendMessage(Texts.setStyleIfAbsent(
                                        Text.literal("Failed to reload block-blacklist.json — check server log."),
                                        Style.EMPTY.withFormatting(Formatting.RED)));
                                return 0;
                            }
                            RestrictedItemEnforcer.enforceAll(source.getServer());
                            source.sendMessage(Texts.setStyleIfAbsent(
                                    Text.literal("Reloaded block-blacklist.json: "
                                            + InteractionGuard.getBlacklistedBlockCount() + " blocks, "
                                            + InteractionGuard.getBlacklistedItemCount() + " items, "
                                            + InteractionGuard.getHardCodedGroupCount() + " hard-coded groups."),
                                    Style.EMPTY.withFormatting(Formatting.GREEN)));
                            return 1;
                        })
                )
        );
    }

    private static int toggleWaterSpread(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        if (MetaTweaks.waterSpreaders.contains(player)) {
            MetaTweaks.waterSpreaders.remove(player);
            source.sendMessage(Texts.setStyleIfAbsent(Text.literal("Water spread disabled."), Style.EMPTY.withFormatting(Formatting.RED)));
        } else {
            MetaTweaks.waterSpreaders.add(player);
            source.sendMessage(Texts.setStyleIfAbsent(Text.literal("Water spread enabled."), Style.EMPTY.withFormatting(Formatting.GREEN)));
        }
        return 1;
    }
}
