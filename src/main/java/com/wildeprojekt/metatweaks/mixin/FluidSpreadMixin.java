package com.wildeprojekt.metatweaks.mixin;


import com.wildeprojekt.metatweaks.MetaTweaks;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Controls fluid spread updates. When MetaTweaks.disableWaterSpread is enabled, this mixin cancels
 * fluid scheduled ticks globally to prevent water/lava from spreading. It provides a whitelist
 * mechanism: if the fluid update position lies within a WorldEdit selection of any player present
 * in MetaTweaks.waterSpreaders, the update is allowed and not cancelled.
 *
 * Implementation details:
 * - Target: FluidState#onScheduledTick via the invocation of Fluid#onScheduledTick.
 * - Logic: Permit spread only when {@link MetaTweaks#isWaterSpreadAllowedAt} reports the BlockPos
 *   is inside a whitelisted player's complete WorldEdit selection. Otherwise, cancel.
 */
@Mixin(FluidState.class)
public class FluidSpreadMixin {

    /**
     * Intercepts fluid scheduled tick right before delegating to Fluid#onScheduledTick. If water spread
     * is globally disabled and the position is not within a whitelisted WorldEdit selection, cancel the tick.
     */
    @Inject(method = "onScheduledTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/Fluid;onScheduledTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/fluid/FluidState;)V"), cancellable = true)
    public void onWaterSpread(World world, BlockPos pos, CallbackInfo ci) {
        if (MetaTweaks.disableWaterSpread && !MetaTweaks.isWaterSpreadAllowedAt(pos)) {
            ci.cancel();
        }
    }
}
