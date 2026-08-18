package net.shelmarow.mine_chat.network.packet.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;
import net.shelmarow.mine_chat.network.PicturePacketReceiver;
import net.shelmarow.mine_chat.network.PicturePacketManager;
import org.jetbrains.annotations.NotNull;

public record C2SSendPicturePacket(
        String hash,
        byte[] data,
        int index,
        int totalPacket,
        int totalLength,
        boolean isGif

) implements CustomPacketPayload {

    public static final Type<C2SSendPicturePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "send_picture_to_server"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSendPicturePacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        ByteBufCodecs.STRING_UTF8.encode(buf, packet.hash);
                        ByteBufCodecs.byteArray(PicturePacketManager.MAX_PACKET_SIZE).encode(buf, packet.data);
                        ByteBufCodecs.INT.encode(buf, packet.index);
                        ByteBufCodecs.INT.encode(buf, packet.totalPacket);
                        ByteBufCodecs.INT.encode(buf, packet.totalLength);
                        ByteBufCodecs.BOOL.encode(buf, packet.isGif);
                    },
                    buf -> {
                        String hash = ByteBufCodecs.STRING_UTF8.decode(buf);
                        byte[] data = ByteBufCodecs.byteArray(PicturePacketManager.MAX_PACKET_SIZE).decode(buf);
                        int index = ByteBufCodecs.INT.decode(buf);
                        int totalPacket = ByteBufCodecs.INT.decode(buf);
                        int totalLength = ByteBufCodecs.INT.decode(buf);
                        boolean isGif = ByteBufCodecs.BOOL.decode(buf);
                        return new C2SSendPicturePacket(hash, data, index, totalPacket, totalLength, isGif);
                    }
            );




    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void handle(C2SSendPicturePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            //将图片发送到服务端
            if(!ServerPictureManager.getInstance().hasPicture(packet.hash)){
                PicturePacketReceiver.getInstance().receivePacket(packet);
            }
        });
    }
}
