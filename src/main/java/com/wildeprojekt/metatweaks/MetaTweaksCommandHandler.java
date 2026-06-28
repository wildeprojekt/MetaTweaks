package com.wildeprojekt.metatweaks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sk89q.worldedit.fabric.FabricWorldEdit;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

/**
 * Registers and implements commands for the MetaTweaks mod.
 * Commands:
 * - /allowwaterspread: Toggles custom water spread behavior (permission: metatweaks.allowwaterspread).
 */
public class MetaTweaksCommandHandler {

    /**
     * Registers all MetaTweaks commands on the provided dispatcher. Only called on dedicated servers.
     * @param dispatcher Brigadier command dispatcher
     * @param registryAccess registry access
     * @param environment registration environment
     */
    public static void MetaTweaksCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment)
    {

        FabricWorldEdit.inst.getPermissionsProvider().registerPermission("metatweaks.allowwaterspread");

        //register allowwaterspread
        dispatcher.register(CommandManager.literal("allowwaterspread").requires(serverCommandSource -> {
                            try {
                                return FabricWorldEdit.inst.getPermissionsProvider().hasPermission(serverCommandSource.getPlayerOrThrow(), "metatweaks.allowwaterspread");
                            } catch (CommandSyntaxException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            if (MetaTweaks.waterSpreaders.contains(source.getPlayer())) {
                                MetaTweaks.waterSpreaders.remove(source.getPlayer());
                                source.sendMessage(Texts.setStyleIfAbsent(Text.literal("Water spread disabled."), Style.EMPTY.withFormatting(Formatting.RED)));
                            } else {
                                MetaTweaks.waterSpreaders.add(source.getPlayer());
                                source.sendMessage(Texts.setStyleIfAbsent(Text.literal("Water spread enabled."), Style.EMPTY.withFormatting(Formatting.GREEN)));
                            }
                            return 1;
                        })
        );
    }
}
