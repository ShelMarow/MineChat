package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.network.PicturePacketReceiver;
import net.shelmarow.mine_chat.network.PicturePacketManager;
import org.jetbrains.annotations.NotNull;

public record S2CSendPicturePacket (
        String hash,
        byte[] data,
        int index,
        int totalPacket,
        int totalLength,
        boolean isGif

) implements CustomPacketPayload {

    public static final Type<S2CSendPicturePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "send_picture_to_client"
            ));


    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSendPicturePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    S2CSendPicturePacket::hash,
                    ByteBufCodecs.byteArray(PicturePacketManager.MAX_PACKET_SIZE),
                    S2CSendPicturePacket::data,
                    ByteBufCodecs.INT,
                    S2CSendPicturePacket::index,
                    ByteBufCodecs.INT,
                    S2CSendPicturePacket::totalPacket,
                    ByteBufCodecs.INT,
                    S2CSendPicturePacket::totalLength,
                    ByteBufCodecs.BOOL,
                    S2CSendPicturePacket::isGif,
                    S2CSendPicturePacket::new
            );


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void handle(S2CSendPicturePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            PicturePacketReceiver.getInstance().receivePacket(packet);
        });
    }
}
