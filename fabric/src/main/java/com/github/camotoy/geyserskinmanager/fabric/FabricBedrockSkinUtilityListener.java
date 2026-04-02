package com.github.camotoy.geyserskinmanager.fabric;

import com.github.camotoy.geyserskinmanager.common.Constants;
import com.github.camotoy.geyserskinmanager.common.RawSkin;
import com.github.camotoy.geyserskinmanager.common.SkinDatabase;
import com.github.camotoy.geyserskinmanager.common.platform.BedrockSkinUtilityListener;
import com.github.camotoy.geyserskinmanager.common.skinretriever.BedrockSkinRetriever;
import net.fabricmc.fabric.api.networking.v1.ClientboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class FabricBedrockSkinUtilityListener extends BedrockSkinUtilityListener<ServerPlayer> {

    public record BedrockSkinPayload(byte[] data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BedrockSkinPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.parse(Constants.MOD_PLUGIN_MESSAGE_NAME));
        public static final StreamCodec<FriendlyByteBuf, BedrockSkinPayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBytes(payload.data()),
                buf -> {
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    return new BedrockSkinPayload(bytes);
                }
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public FabricBedrockSkinUtilityListener(SkinDatabase database, BedrockSkinRetriever skinRetriever) {
        super(database, skinRetriever);

        PayloadTypeRegistry.clientboundPlay().register(BedrockSkinPayload.TYPE, BedrockSkinPayload.CODEC);

        ClientboundPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (channels.contains(BedrockSkinPayload.TYPE.id())) {
                server.execute(() -> onModdedPlayerConfirm(handler.getPlayer()));
            }
        });
    }

    @Override
    public void sendPluginMessage(byte[] payload, ServerPlayer player) {
        ServerPlayNetworking.send(player, new BedrockSkinPayload(payload));
    }

    @Override
    public UUID getUUID(ServerPlayer player) {
        return player.getUUID();
    }
}
