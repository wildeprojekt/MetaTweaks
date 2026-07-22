package com.wildeprojekt.metatweaks;

import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;

public final class TeleportGuard {

    private static final ThreadLocal<String> CURRENT_COMMAND = new ThreadLocal<>();

    private TeleportGuard() {
    }

    public static void setCurrentCommand(String command) {
        CURRENT_COMMAND.set(command);
    }

    public static void clearCurrentCommand() {
        CURRENT_COMMAND.remove();
    }

    public static boolean hasFullAccess(ServerCommandSource source) {
        if (source.hasPermissionLevel(2)) {
            return true;
        }
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return true;
        }
        return MetaTweaks.hasBypass(player);
    }

    /**
     * @return {@code true} if the teleport may proceed
     */
    public static boolean allow(ServerCommandSource source, Collection<? extends Entity> targets) {
        if (hasFullAccess(source)) {
            return true;
        }

        String command = CURRENT_COMMAND.get();
        if (command != null && command.indexOf('@') >= 0) {
            CommandMessages.send(source, "metatweaks.command.teleport.no_selectors");
            return false;
        }

        Entity self = source.getEntity();
        for (Entity target : targets) {
            if (target != self) {
                CommandMessages.send(source, "metatweaks.command.teleport.self_only");
                return false;
            }
        }
        return true;
    }
}
