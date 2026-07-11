package com.wildeprojekt.metatweaks.mixin;

import com.wildeprojekt.metatweaks.MetaTweaks;
import com.wildeprojekt.metatweaks.RestrictedItemEnforcer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(net.minecraft.server.network.ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "stopRiding()V", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void stopRidingInject(CallbackInfo ci, Entity entity) {
        try {
            if (entity != null && entity.getCustomName() != null && entity.getCustomName().getString().equals("deleteme")) {
                entity.discard();
            }
        } catch (Exception ignored) {
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"), method = "playerTick", locals = LocalCapture.CAPTURE_FAILHARD)
    private void metatweaks$stripInventorySlot(CallbackInfo ci, int i) {
        net.minecraft.server.network.ServerPlayerEntity player = (net.minecraft.server.network.ServerPlayerEntity) (Object) this;
        if (MetaTweaks.hasBypass(player)) {
            return;
        }
        ItemStack stack = player.getInventory().getStack(i);
        RestrictedItemEnforcer.stripStack(player, stack);
    }

    @Inject(method = "playerTick", at = @At("TAIL"))
    private void metatweaks$stripEquipmentAndCursor(CallbackInfo ci) {
        RestrictedItemEnforcer.stripEquipmentAndCursor((net.minecraft.server.network.ServerPlayerEntity) (Object) this);
    }
}
