package com.wildeprojekt.metatweaks;

import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.fabric.player.FabricPlayer;
import com.plotsquared.fabric.util.FabricUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

final class PlotBuildGuard {

    enum BuildCheckResult {
        ALLOW,
        DENY
    }

    private PlotBuildGuard() {
    }

    static BuildCheckResult canBuildAt(ServerPlayerEntity player, RegistryKey<World> worldKey, BlockPos pos) {
        Location location = FabricUtil.adapt(GlobalPos.create(worldKey, pos));
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return MetaTweaks.hasPermission(player, "metatweaks.build")
                    ? BuildCheckResult.ALLOW
                    : BuildCheckResult.DENY;
        }

        FabricPlayer fabricPlayer = FabricUtil.adapt(player);
        Plot plot = area.getPlot(location);
        if (plot == null) {
            return BuildCheckResult.DENY;
        }

        if (area.notifyIfOutsideBuildArea(fabricPlayer, location.getY())) {
            fabricPlayer.sendMessage(
                    TranslatableCaption.of("height.height_limit"),
                    TagResolver.builder()
                            .tag("minheight", Tag.inserting(Component.text(area.getMinBuildHeight())))
                            .tag("maxheight", Tag.inserting(Component.text(area.getMaxBuildHeight())))
                            .build()
            );
            return BuildCheckResult.DENY;
        }

        if (!plot.hasOwner()) {
            if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                fabricPlayer.sendMessage(
                        TranslatableCaption.of("permission.no_permission_event"),
                        TagResolver.resolver("node",
                                Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_UNOWNED))
                );
                return BuildCheckResult.DENY;
            }
        } else {
            boolean isOwner = plot.getOwner() != null && plot.getOwner().equals(fabricPlayer.getUUID());
            boolean isAdded = plot.isAdded(fabricPlayer.getUUID());
            if (!(isOwner || isAdded)) {
                if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                    fabricPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            TagResolver.resolver("node",
                                    Tag.inserting(Permission.PERMISSION_ADMIN_BUILD_OTHER))
                    );
                    return BuildCheckResult.DENY;
                }
            }
        }

        if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
            if (!fabricPlayer.hasPermission(Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                fabricPlayer.sendMessage(TranslatableCaption.of("done.building_restricted"));
                return BuildCheckResult.DENY;
            }
        }

        return BuildCheckResult.ALLOW;
    }
}
