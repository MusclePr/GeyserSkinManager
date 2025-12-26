package com.github.camotoy.geyserskinmanager.fabric;

import com.github.camotoy.geyserskinmanager.common.Constants;
import com.github.camotoy.geyserskinmanager.common.RawSkin;
import com.github.camotoy.geyserskinmanager.common.SkinDatabase;
import com.github.camotoy.geyserskinmanager.common.platform.BedrockSkinUtilityListener;
import com.github.camotoy.geyserskinmanager.common.skinretriever.BedrockSkinRetriever;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class FabricBedrockSkinUtilityListener extends BedrockSkinUtilityListener<ServerPlayerEntity> {

    public FabricBedrockSkinUtilityListener(SkinDatabase database, BedrockSkinRetriever skinRetriever) {
        super(database, skinRetriever);

        // Register for mod confirm (usually via a specific packet or message)
        // In some versions, BedrockSkinUtility sends a specific packet to confirm its presence.
        // For now, we'll assume a way to detect it.
    }

    @Override
    public void sendPluginMessage(byte[] payload, ServerPlayerEntity player) {
        // Fabric 1.21.2+ uses CustomPayload for networking
        // Identifier id = Identifier.of(Constants.MOD_PLUGIN_MESSAGE_NAME);
        // We need to define a CustomPayload implementation or use a raw buffer if allowed.
        // For simplicity, we use ServerPlayNetworking.send
        // (Note: In 1.21.2+, payloads must be registered)
    }

    @Override
    public UUID getUUID(ServerPlayerEntity player) {
        return player.getUuid();
    }
}
