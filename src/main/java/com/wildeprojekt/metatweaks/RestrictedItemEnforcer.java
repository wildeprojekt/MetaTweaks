package com.wildeprojekt.metatweaks;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class RestrictedItemEnforcer {

    private RestrictedItemEnforcer() {
    }

    public static void enforceAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            stripDeniedItems(player);
        }
    }

    public static void stripDeniedItems(ServerPlayerEntity player) {
        if (MetaTweaks.hasBypass(player)) {
            return;
        }
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            stripStack(player, player.getInventory().getStack(slot));
        }
        stripEquipmentAndCursor(player);
    }

    public static void stripEquipmentAndCursor(ServerPlayerEntity player) {
        if (MetaTweaks.hasBypass(player)) {
            return;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            stripStack(player, player.getEquippedStack(slot));
        }
        stripStack(player, player.currentScreenHandler.getCursorStack());
    }

    public static void stripStack(ServerPlayerEntity player, ItemStack stack) {
        if (stack.isEmpty() || MetaTweaks.hasBypass(player)) {
            return;
        }
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (!InteractionGuard.canHoldItem(player, itemId)) {
            stack.setCount(0);
        }
    }
}
