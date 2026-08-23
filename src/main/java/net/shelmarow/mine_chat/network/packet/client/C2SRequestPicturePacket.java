package net.shelmarow.mine_chat.network.packet.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;
import org.jetbrains.annotations.NotNull;

public record C2SRequestPicturePacket(String hash) implements CustomPacketPayload {

    public static final Type<C2SRequestPicturePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "request_picture"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestPicturePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    C2SRequestPicturePacket::hash,
                    C2SRequestPicturePacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SRequestPicturePacket packet, IPayloadContext context) {
        context.enqueueWork(()->{
            ServerPictureManager.getInstance().requestPictureToClient(packet.hash(), context.player().getUUID());
        });
    }
}
