package com.wildeprojekt.metatweaks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.projectile.ProjectileHitEvent;
import xyz.nucleoid.stimuli.event.world.*;

import java.util.ArrayList;
import java.util.HashSet;

public class MetaTweaks implements ModInitializer {

    public static ArrayList<ServerPlayerEntity> paintingBreakers;
    public static boolean disableWaterSpread = true;
    public static HashSet<ServerPlayerEntity> waterSpreaders;
    public static boolean eventBypass = false;

    /**
     * Fabric mod initialization hook. Sets up static state, interaction rules config,
     * registers Stimuli listeners, command handlers, and protection logic.
     */
    @Override
    public void onInitialize() {
        waterSpreaders = new HashSet<>();
        paintingBreakers = new ArrayList<>();
        InteractionGuard.load();
        /*
         * Projectile entity hit handling.
         * - Default: deny projectile collisions with entities (returns FAIL) to prevent grief (e.g., arrows breaking frames).
         * - When eventBypass is true: allow projectile hits in general (SUCCESS) but still deny impacts on paintings and item frames.
         */
        Stimuli.global().listen(ProjectileHitEvent.ENTITY, (projectileEntity, hitResult) -> {
            if (eventBypass) {
                if (hitResult.getEntity().getType().getLootTableId().equals(new Identifier("minecraft:entities/painting"))) {
                    return ActionResult.FAIL;
                }
                if (hitResult.getEntity().getType().getLootTableId().equals(new Identifier("conquest:entities/painting"))) {
                    return ActionResult.FAIL;
                }
                if (hitResult.getEntity().getType().getLootTableId().equals(new Identifier("minecraft:entities/item_frame"))) {
                    return ActionResult.FAIL;
                }

                return ActionResult.SUCCESS;
            }
            return ActionResult.FAIL;
        });


        /*
         * Disable fire tick updates globally (no natural fire spread or block ignition updates).
         */
        Stimuli.global().listen(FireTickEvent.EVENT, (world, pos) -> {
            return ActionResult.FAIL;
        });

        /*
         * Prevent ice from melting into water.
         */
        Stimuli.global().listen(IceMeltEvent.EVENT, (world, pos) -> {
            return ActionResult.FAIL;
        });

        /*
         * Block Wither summoning sequences from completing.
         */
        Stimuli.global().listen(WitherSummonEvent.EVENT, (world, pos) -> {
            return ActionResult.FAIL;
        });

        /*
         * Prevent new snow layers from forming due to snowfall.
         */
        Stimuli.global().listen(SnowFallEvent.EVENT, (world, pos) -> {
            return ActionResult.FAIL;
        });

        /*
         * Prevent TNT from being ignited by any source.
         */
        Stimuli.global().listen(TntIgniteEvent.EVENT, (world, pos, entity) -> {
            return ActionResult.FAIL;
        });


        /*
         * Explosion detonation listener: intentionally left as a no-op placeholder for future logic.
         */
        Stimuli.global().listen(ExplosionDetonatedEvent.EVENT, (explosion, particles) -> {
            return;
        });

        /*
         * Register MetaTweaks commands.
         */
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MetaTweaksCommandHandler.MetaTweaksCommands(dispatcher, registryAccess, environment);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                return LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(player).getCachedData().getPermissionData().checkPermission("metatweaks.candie").asBoolean();
            }
            return true;
        });

        /*
         * Damage permission gate for players.
         * - Players require `metatweaks.candie` to receive damage (returns true to allow damage).
         * - Non-player entities are unaffected (damage allowed).
         */
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (hasPermission(player, "metatweaks.candie")) {
                    return true;
                }
                return false;
            }
            return true;
        });

        /*
         * Block break: global block blacklist. Blacklisted blocks require metatweaks.bypass.
         */
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                Identifier blockId = Registries.BLOCK.getId(state.getBlock());
                if (!InteractionGuard.canInteractBlock(serverPlayer, blockId)) {
                    return false;
                }
            }
            return true;
        });

        /*
         * Block break: general build gate. Requires metatweaks.build to break any block.
         */
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!hasPermission(serverPlayer, "metatweaks.build")) {
                    return false;
                }
            }
            return true;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (player instanceof ServerPlayerEntity serverPlayer) {
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                if (!InteractionGuard.canUseItem(serverPlayer, itemId)) {
                    return ActionResult.FAIL;
                }
                Identifier blockId = Registries.BLOCK.getId(world.getBlockState(hitResult.getBlockPos()).getBlock());
                if (!InteractionGuard.canInteractBlock(serverPlayer, blockId)) {
                    return ActionResult.FAIL;
                }
                return PlotBuildGuard.canBuildAt(
                        serverPlayer,
                        serverPlayer.getServerWorld().getRegistryKey(),
                        hitResult.getBlockPos()
                ) == PlotBuildGuard.BuildCheckResult.ALLOW
                        ? ActionResult.PASS
                        : ActionResult.FAIL;
            }

            return isBlockProtectedAgainstUseAction(player, world, hand, hitResult) ? ActionResult.FAIL : ActionResult.PASS;
        });


        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (!hasPermission(player, "metatweaks.build")) {
                return ActionResult.FAIL;
            }

            if (player instanceof ServerPlayerEntity serverPlayer) {
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                if (!InteractionGuard.canUseItem(serverPlayer, itemId)) {
                    return ActionResult.FAIL;
                }
            }

            return ActionResult.PASS;
        });


        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (Registries.ITEM.getId(stack.getItem()).toString().startsWith("patchouli:guide_book")) {
                return TypedActionResult.pass(stack);
            }

            if (player instanceof ServerPlayerEntity serverPlayer) {
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                if (!InteractionGuard.canUseItem(serverPlayer, itemId)) {
                    return TypedActionResult.fail(ItemStack.EMPTY);
                }
                return PlotBuildGuard.canBuildAt(
                        serverPlayer,
                        serverPlayer.getServerWorld().getRegistryKey(),
                        serverPlayer.getBlockPos()
                ) == PlotBuildGuard.BuildCheckResult.ALLOW
                        ? TypedActionResult.pass(stack)
                        : TypedActionResult.fail(ItemStack.EMPTY);
            }

            return TypedActionResult.pass(ItemStack.EMPTY);
        });
        
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            return PlotBuildGuard.canBuildAt(
                    serverPlayer,
                    serverPlayer.getServerWorld().getRegistryKey(),
                    pos
            ) == PlotBuildGuard.BuildCheckResult.ALLOW
                    ? ActionResult.PASS
                    : ActionResult.FAIL;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
                if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                    return ActionResult.PASS;
                }

                return PlotBuildGuard.canBuildAt(
                        serverPlayer,
                        serverPlayer.getServerWorld().getRegistryKey(),
                        entity.getBlockPos()
                ) == PlotBuildGuard.BuildCheckResult.ALLOW
                        ? ActionResult.PASS
                        : ActionResult.FAIL;
        });
    }

    /**
     * Checks if both hands are empty.
     *
     * @param player the player to check
     * @return true if both main hand and offhand are empty, false otherwise
     */
    public boolean isHandEmpty(PlayerEntity player) {
        return player.getMainHandStack().isEmpty() && player.getOffHandStack().isEmpty();
    }

    /**
     * Checks a LuckPerms permission for the given player. Only evaluated for server players.
     *
     * @param player     the player
     * @param permission the permission node to check
     * @return true if permitted; false otherwise or if not a server player/adapter unavailable
     */
    public static boolean hasPermission(PlayerEntity player, String permission) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            try {
                if (LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverPlayer).getCachedData().getPermissionData().checkPermission(permission).asBoolean()) {
                    return true;
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
    public static boolean hasBypass(ServerPlayerEntity player) {
        return hasPermission(player, "metatweaks.bypass");
    }


    public static boolean hasCreate(ServerPlayerEntity player) {
        return hasPermission(player, "metatweaks.create") || hasBypass(player);
    }


    public static boolean hasAllowWaterSpread(ServerPlayerEntity player) {
        return hasPermission(player, "metatweaks.allowwaterspread") || hasBypass(player);
    }

    /**
     * Determines whether a block use action should be treated as protected/denied for a player.
     * Allows bare-hand interaction with doors/gates; otherwise requires metatweaks.build.
     *
     * @param player    the player
     * @param world     the world
     * @param hand      the hand used
     * @param hitResult the targeted block hit
     * @return true if use action is protected (should be denied), false otherwise
     */
    public boolean isBlockProtectedAgainstUseAction(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {

        var blockName = Registries.BLOCK.getId(world.getBlockState(hitResult.getBlockPos()).getBlock()).toString().toLowerCase();

        if (isHandEmpty(player) && (blockName.endsWith("_door") || blockName.endsWith("_gate"))) return false;
        return !hasPermission(player, "metatweaks.build");
    }
}
