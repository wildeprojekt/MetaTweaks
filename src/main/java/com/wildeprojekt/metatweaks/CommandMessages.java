package com.wildeprojekt.metatweaks;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public final class CommandMessages {

    private CommandMessages() {
    }

    public static void send(ServerCommandSource source, String key, Object... args) {
        source.sendMessage(Text.translatable(key, args));
    }
}
