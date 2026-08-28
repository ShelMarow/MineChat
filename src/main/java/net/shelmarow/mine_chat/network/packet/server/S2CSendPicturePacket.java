package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.PictureFormat;
import net.shelmarow.mine_chat.chat.picture.PicturePacketManager;
import net.shelmarow.mine_chat.chat.picture.PicturePacketReceiver;
import net.shelmarow.mine_chat.config.MineChatServerConfig;
import org.jetbrains.annotations.NotNull;

public record S2CSendPicturePacket (
        String hash,
        byte[] data,
        int index,
        int totalPacket,
        int totalLength,
        PictureFormat format

) implements CustomPacketPayload {

    public static final Type<S2CSendPicturePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "send_picture_to_client"
            ));


    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSendPicturePacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, packet.hash());
                ByteBufCodecs.byteArray(PicturePacketManager.MAX_PACKET_SIZE).encode(buf, packet.data());
                ByteBufCodecs.INT.encode(buf, packet.index());
                ByteBufCodecs.INT.encode(buf, packet.totalPacket());
                ByteBufCodecs.INT.encode(buf, packet.totalLength());

                ByteBufCodecs.VAR_INT.encode(buf, packet.format().ordinal());
            }, (buf) -> {
                String hash = ByteBufCodecs.STRING_UTF8.decode(buf);
                byte[] data = ByteBufCodecs.byteArray(PicturePacketManager.MAX_PACKET_SIZE).decode(buf);
                int index = ByteBufCodecs.INT.decode(buf);
                int totalPacket = ByteBufCodecs.INT.decode(buf);
                int totalLength = ByteBufCodecs.INT.decode(buf);

                int ordinal = ByteBufCodecs.VAR_INT.decode(buf);
                PictureFormat format = PictureFormat.values()[ordinal];

                return new S2CSendPicturePacket(hash, data, index, totalPacket, totalLength, format);
            });


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void handle(S2CSendPicturePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if(MineChatServerConfig.ENABLE_NETWORK_PICTURE.get()){
                PicturePacketReceiver.getInstance().receivePacket(packet);
            }
        });
    }
}
