package com.github.camotoy.geyserskinmanager.fabric;

import com.github.camotoy.geyserskinmanager.common.SkinEntry;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;

public class FabricSkinApplier {
    private static final Logger LOGGER = LoggerFactory.getLogger("geyserskinmanager-fabric-applier");

    public void setSkin(ServerPlayerEntity player, SkinEntry skinEntry) {
        GameProfile profile = player.getGameProfile();

        // Remove existing textures
        profile.getProperties().removeAll("textures");
        // Apply new skin
        profile.getProperties().put("textures",
                new Property("textures", skinEntry.getJavaSkinValue(), skinEntry.getJavaSkinSignature()));

        syncSkin(player);
    }

    private void syncSkin(ServerPlayerEntity player) {
        // In 1.21.2+, we need to update the player list and re-spawn the entity to
        // reflect skin changes to other players.
        // For the player themselves, they might need a respawn or just a packet update.

        MinecraftServer server = player.getServer();
        if (server == null)
            return;

        PlayerManager playerManager = server.getPlayerManager();

        // 1. Update player list for all players
        // In 1.21.2+, PlayerListS2CPacket.Action is often replaced or supplemented by
        // PlayerInfoUpdateS2CPacket
        // Fabric API provides some level of abstraction, but here we use NMS/Yarn
        // mappings.
        playerManager.sendToAll(new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.Action.UPDATE_LISTED),
                Collections.singletonList(player)));

        // 2. Hide and show the player to others to force skin reload
        for (ServerPlayerEntity other : playerManager.getPlayerList()) {
            if (other == player)
                continue;
            if (other.canSee(player)) {
                // Simulate hide/show
                other.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(player.getId()));
                other.networkHandler.sendPacket(new EntitySpawnS2CPacket(
                        player.getId(),
                        player.getUuid(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.getPitch(),
                        player.getYaw(),
                        player.getType(),
                        0,
                        player.getVelocity(),
                        player.getHeadYaw()));
            }
        }

        LOGGER.info("Skin synced for player: " + player.getName().getString());
    }
}
