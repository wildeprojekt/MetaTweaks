package com.wildeprojekt.metatweaks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

final class RestrictedItemEnforcer {

    private RestrictedItemEnforcer() {
    }

    static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> enforce(handler.player));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) {
                return;
            }
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                enforce(player);
            }
        });
    }

    static void enforce(ServerPlayerEntity player) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (removeIfDenied(player, player.getInventory().getStack(slot))) {
                player.getInventory().setStack(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getEquippedStack(slot);
            if (removeIfDenied(player, stack)) {
                player.equipStack(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            player.currentScreenHandler.sendContentUpdates();
        }
    }

    private static boolean removeIfDenied(ServerPlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        return !InteractionGuard.canHoldItem(player, itemId);
    }
}
