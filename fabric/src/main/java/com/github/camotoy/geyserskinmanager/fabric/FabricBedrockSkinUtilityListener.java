package com.github.camotoy.geyserskinmanager.fabric;

import com.github.camotoy.geyserskinmanager.common.Constants;
import com.github.camotoy.geyserskinmanager.common.RawSkin;
import com.github.camotoy.geyserskinmanager.common.SkinDatabase;
import com.github.camotoy.geyserskinmanager.common.platform.BedrockSkinUtilityListener;
import com.github.camotoy.geyserskinmanager.common.skinretriever.BedrockSkinRetriever;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class FabricBedrockSkinUtilityListener extends BedrockSkinUtilityListener<ServerPlayerEntity> {

    public record BedrockSkinPayload(byte[] data) implements CustomPayload {
        public static final Id<BedrockSkinPayload> ID = new Id<>(Identifier.of(Constants.MOD_PLUGIN_MESSAGE_NAME));
        public static final PacketCodec<PacketByteBuf, BedrockSkinPayload> CODEC = CustomPayload.codecOf(
                (payload, buf) -> buf.writeBytes(payload.data()),
                buf -> {
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    return new BedrockSkinPayload(bytes);
                }
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public FabricBedrockSkinUtilityListener(SkinDatabase database, BedrockSkinRetriever skinRetriever) {
        super(database, skinRetriever);

        PayloadTypeRegistry.playS2C().register(BedrockSkinPayload.ID, BedrockSkinPayload.CODEC);

        S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (channels.contains(BedrockSkinPayload.ID.id())) {
                server.execute(() -> onModdedPlayerConfirm(handler.getPlayer()));
            }
        });
    }

    @Override
    public void sendPluginMessage(byte[] payload, ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new BedrockSkinPayload(payload));
    }

    @Override
    public UUID getUUID(ServerPlayerEntity player) {
        return player.getUuid();
    }
}
