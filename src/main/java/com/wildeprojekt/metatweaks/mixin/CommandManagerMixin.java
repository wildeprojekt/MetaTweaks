package com.wildeprojekt.metatweaks.mixin;

import com.mojang.brigadier.ParseResults;
import com.wildeprojekt.metatweaks.TeleportGuard;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandManager.class)
public class CommandManagerMixin {

    @Inject(method = "execute", at = @At("HEAD"))
    private void metatweaks$captureCommand(
            ParseResults<ServerCommandSource> parseResults,
            String command,
            CallbackInfoReturnable<Integer> cir
    ) {
        TeleportGuard.setCurrentCommand(command);
    }

    @Inject(method = "execute", at = @At("RETURN"))
    private void metatweaks$clearCommand(
            ParseResults<ServerCommandSource> parseResults,
            String command,
            CallbackInfoReturnable<Integer> cir
    ) {
        TeleportGuard.clearCurrentCommand();
    }
}
