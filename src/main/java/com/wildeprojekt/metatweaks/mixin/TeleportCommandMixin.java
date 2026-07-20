package com.wildeprojekt.metatweaks.mixin;

import com.wildeprojekt.metatweaks.TeleportGuard;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeleportCommand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.command.argument.PosArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.function.Predicate;

@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {

    @ModifyArg(
            method = "register",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;requires(Ljava/util/function/Predicate;)Lcom/mojang/brigadier/builder/ArgumentBuilder;",
                    ordinal = 0
            ),
            index = 0
    )
    private static Predicate<ServerCommandSource> metatweaks$allowTpWithoutOp(Predicate<ServerCommandSource> original) {
        return source -> true;
    }

    @Inject(
            method = "execute(Lnet/minecraft/server/command/ServerCommandSource;Ljava/util/Collection;Lnet/minecraft/entity/Entity;)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void metatweaks$guardEntityTeleport(
            ServerCommandSource source,
            Collection<? extends Entity> targets,
            Entity destination,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!TeleportGuard.allow(source, targets)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(
            method = "execute(Lnet/minecraft/server/command/ServerCommandSource;Ljava/util/Collection;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/command/argument/PosArgument;Lnet/minecraft/command/argument/PosArgument;Lnet/minecraft/server/command/TeleportCommand$LookTarget;)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void metatweaks$guardPosTeleport(
            ServerCommandSource source,
            Collection<? extends Entity> targets,
            ServerWorld world,
            PosArgument location,
            PosArgument rotation,
            TeleportCommand.LookTarget facing,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!TeleportGuard.allow(source, targets)) {
            cir.setReturnValue(0);
        }
    }
}
