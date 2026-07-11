package com.wildeprojekt.metatweaks.mixin;

import com.wildeprojekt.metatweaks.RestrictedItemEnforcer;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class SlotClickInventoryMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onClickSlot", at = @At("TAIL"))
    private void metatweaks$enforceAfterSlotClick(ClickSlotC2SPacket packet, CallbackInfo ci) {
        RestrictedItemEnforcer.enforce(player);
    }
}
