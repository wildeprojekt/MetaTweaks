package com.wildeprojekt.metatweaks.mixin;

import com.wildeprojekt.metatweaks.MetaTweaks;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /**
     * Prevents entity collisions for players without the "metatweaks.allow.entitypush" permission.
     */
    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void preventEntityCollisions(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ServerPlayerEntity player && !MetaTweaks.hasPermission(player, "metatweaks.allow.entitypush")) {
            ci.cancel();
        } else if (other instanceof ServerPlayerEntity player && !MetaTweaks.hasPermission(player, "metatweaks.allow.entitypush")) {
            ci.cancel();
        }
    }
}
