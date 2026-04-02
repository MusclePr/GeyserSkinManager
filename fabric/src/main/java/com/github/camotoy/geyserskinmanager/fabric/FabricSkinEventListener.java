package com.github.camotoy.geyserskinmanager.fabric;

import com.github.camotoy.geyserskinmanager.common.SkinEntry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public class FabricSkinEventListener extends com.github.camotoy.geyserskinmanager.common.platform.SkinEventListener<ServerPlayer, MinecraftServer> {
    private final FabricSkinApplier skinApplier;

    private final FabricBedrockSkinUtilityListener modListener;

    public FabricSkinEventListener(Path skinDatabaseLocation, boolean provideTargetSkins) {
        super(skinDatabaseLocation, GeyserSkinManager.LOGGER::warn);
        this.skinApplier = provideTargetSkins ? new FabricSkinApplier() : null;
        this.modListener = new FabricBedrockSkinUtilityListener(this.database, this.skinRetriever);

        // Register the events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            onPlayerJoin(handler.getPlayer(), server);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            modListener.onPlayerLeave(handler.getPlayer());
        });
    }

    public void onPlayerJoin(ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        com.github.camotoy.geyserskinmanager.common.RawSkin skin = skinRetriever.getBedrockSkin(player.getUUID());
        if (skin != null && this.skinApplier != null) {
            com.mojang.authlib.GameProfile gameProfile = player.getGameProfile();
            // In 1.21.2+, we can check if textures are already present via properties
            if (gameProfile.properties().get("textures").isEmpty()) {
                uploadOrRetrieveSkin(player, server, skin);
            }
        }

        if (skin != null || skinRetriever.isBedrockPlayer(player.getUUID())) {
            modListener.onBedrockPlayerJoin(player, skin);
        }
    }

    @Override
    public void onSuccess(ServerPlayer player, net.minecraft.server.MinecraftServer server, SkinEntry skinEntry) {
        if (skinApplier != null) {
            skinApplier.setSkin(player, skinEntry);
        }
    }

    @Override
    public java.util.UUID getUUID(ServerPlayer player) {
        return player.getUUID();
    }
}
