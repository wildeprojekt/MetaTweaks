package com.wildeprojekt.metatweaks.mixin;

import com.wildeprojekt.metatweaks.InteractionGuard;
import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(EntityBucketItem.class)
public class EntityBucketItemMixin {

    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    private void metatweaks$blockDispensedSpawn(ServerWorld world, ItemStack stack, BlockPos pos, CallbackInfo ci) {
        if (isPlayerBucketUse()) {
            return;
        }
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (InteractionGuard.isHardCodedRestricted(stack.getItem())
                || InteractionGuard.isItemBlacklisted(itemId)) {
            ci.cancel();
        }
    }

    private static boolean isPlayerBucketUse() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            String method = frame.getMethodName();
            if ("useOnBlock".equals(method) || "use".equals(method)) {
                return true;
            }
        }
        return false;
    }
}
