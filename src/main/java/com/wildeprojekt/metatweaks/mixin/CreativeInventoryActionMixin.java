package com.wildeprojekt.metatweaks.mixin;

import com.wildeprojekt.metatweaks.RestrictedItemEnforcer;
import com.wildeprojekt.metatweaks.InteractionGuard;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class CreativeInventoryActionMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onCreativeInventoryAction", at = @At("HEAD"), cancellable = true)
    private void metatweaks$blockCreativePick(CreativeInventoryActionC2SPacket packet, CallbackInfo ci) {
        ItemStack stack = ((CreativeInventoryActionC2SPacketAccessor) packet).metatweaks$getStack();
        if (stack.isEmpty()) {
            return;
        }
        if (!InteractionGuard.canHoldItem(player, Registries.ITEM.getId(stack.getItem()))) {
            ci.cancel();
        }
    }

    @Inject(method = "onCreativeInventoryAction", at = @At("TAIL"))
    private void metatweaks$enforceAfterCreativeAction(CreativeInventoryActionC2SPacket packet, CallbackInfo ci) {
        RestrictedItemEnforcer.enforce(player);
    }
}
