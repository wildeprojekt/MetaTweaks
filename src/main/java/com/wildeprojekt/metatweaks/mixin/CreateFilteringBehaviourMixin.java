package com.wildeprojekt.metatweaks.mixin;


import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.wildeprojekt.metatweaks.MetaTweaks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Restricts Create block-entity configuration packets unless the sender has
 * {@code metatweaks.create} (or bypass).
 */
@Mixin(BlockEntityConfigurationPacket.class)
abstract class CreateFilteringBehaviourMixin {

    @Inject(method = "handle", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private void handle(SimplePacketBase.Context context, CallbackInfoReturnable<Boolean> cir) {
        if (!MetaTweaks.hasCreate(context.getSender())) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

}
