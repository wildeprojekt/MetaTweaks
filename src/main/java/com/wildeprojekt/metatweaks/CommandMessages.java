package com.wildeprojekt.metatweaks;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

public final class CommandMessages {

    private CommandMessages() {
    }

    public static void send(ServerCommandSource source, String message, Formatting color) {
        source.sendMessage(prefixed(styled(message, color)));
    }

    public static Text prefixed(Text body) {
        return Text.translatable("debug.prefix").append(body);
    }

    private static Text styled(String message, Formatting color) {
        return Texts.setStyleIfAbsent(Text.literal(message), Style.EMPTY.withFormatting(color));
    }
}
