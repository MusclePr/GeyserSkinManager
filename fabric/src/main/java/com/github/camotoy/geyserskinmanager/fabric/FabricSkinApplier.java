package com.github.camotoy.geyserskinmanager.fabric;

import com.github.camotoy.geyserskinmanager.common.SkinEntry;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;

public class FabricSkinApplier {
    private static final Logger LOGGER = LoggerFactory.getLogger("geyserskinmanager-fabric-applier");

    public void setSkin(ServerPlayer player, SkinEntry skinEntry) {
        GameProfile profile = player.getGameProfile();

        // Remove existing textures
        profile.properties().removeAll("textures");
        // Apply new skin
        profile.properties().put("textures",
                new Property("textures", skinEntry.getJavaSkinValue(), skinEntry.getJavaSkinSignature()));

        syncSkin(player);
    }

    private void syncSkin(ServerPlayer player) {
        // Reflect skin changes to other players by updating the player list and
        // re-spawning the entity for all players currently tracking this player.

        MinecraftServer server = player.level().getServer();
        if (server == null)
            return;

        PlayerList playerList = server.getPlayerList();

        // 1. Update player list for all players
        playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED),
                Collections.singletonList(player)));

        // 2. Hide and show the player to others to force skin reload
        // PlayerLookup.tracking() returns only players who are currently tracking this entity,
        // equivalent to the old canSee() check.
        for (ServerPlayer other : PlayerLookup.tracking(player)) {
            if (other == player)
                continue;
            // Simulate hide/show
            other.connection.send(new ClientboundRemoveEntitiesPacket(player.getId()));
            other.connection.send(new ClientboundAddEntityPacket(
                    player.getId(),
                    player.getUUID(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getXRot(),
                    player.getYRot(),
                    player.getType(),
                    0,
                    player.getDeltaMovement(),
                    player.getYHeadRot()));
        }

        LOGGER.info("Skin synced for player: " + player.getName().getString());
    }
}
