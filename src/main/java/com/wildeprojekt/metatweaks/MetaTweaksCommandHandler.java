package com.wildeprojekt.metatweaks;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Registers and implements commands for the MetaTweaks mod.
 * Commands:
 * - //allowwaterspread: Toggles water spread (metatweaks.allowwaterspread or bypass).
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
        dispatcher.register(CommandManager.literal("/allowwaterspread")
                .requires(MetaTweaksCommandHandler::hasAllowWaterSpreadPermission)
                .executes(context -> toggleWaterSpread(context.getSource())));

        dispatcher.register(CommandManager.literal("metatweaks")
                .then(CommandManager.literal("reload")
                        .requires(MetaTweaksCommandHandler::hasBypassPermission)
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            if (!InteractionGuard.reload()) {
                                CommandMessages.send(source, "metatweaks.command.reload.failed");
                                return 0;
                            }
                            RestrictedItemEnforcer.enforceAll(source.getServer());
                            CommandMessages.send(source, "metatweaks.command.reload.success",
                                    InteractionGuard.getBlacklistedBlockCount(),
                                    InteractionGuard.getBlacklistedItemCount(),
                                    InteractionGuard.getHardCodedGroupCount());
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
            CommandMessages.send(source, "metatweaks.command.waterspread.disabled");
        } else {
            if (!MetaTweaks.hasCompleteWorldEditSelection(player)) {
                CommandMessages.send(source, "metatweaks.command.waterspread.no_selection");
                return 0;
            }
            MetaTweaks.waterSpreaders.add(player);
            CommandMessages.send(source, "metatweaks.command.waterspread.enabled");
        }
        return 1;
    }
}
