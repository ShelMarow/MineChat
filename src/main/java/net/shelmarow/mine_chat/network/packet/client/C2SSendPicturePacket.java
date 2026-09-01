package net.shelmarow.mine_chat.network.packet.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.mine_chat.chat.picture.PictureFormat;
import net.shelmarow.mine_chat.chat.picture.PicturePacketManager;
import net.shelmarow.mine_chat.chat.picture.PicturePacketReceiver;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;
import net.shelmarow.mine_chat.config.MineChatServerConfig;

import java.util.function.Supplier;

public record C2SSendPicturePacket(String hash, byte[] data, int index, int totalPacket, int totalLength,
                                   PictureFormat format) {

    public static C2SSendPicturePacket decode(FriendlyByteBuf buf) {
        String hash = buf.readUtf();
        byte[] data = buf.readByteArray(PicturePacketManager.MAX_PACKET_SIZE);
        int index = buf.readInt();
        int totalPacket = buf.readInt();
        int totalLength = buf.readInt();
        int ordinal = buf.readVarInt();

        PictureFormat[] formats = PictureFormat.values();
        if (ordinal < 0 || ordinal >= formats.length) {
            throw new IllegalArgumentException("Invalid PictureFormat ordinal: " + ordinal);
        }

        PictureFormat format = formats[ordinal];

        return new C2SSendPicturePacket(hash, data, index, totalPacket, totalLength, format);
    }

    public static void handle(C2SSendPicturePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            if(MineChatServerConfig.ENABLE_NETWORK_PICTURE.get()){
                if (!ServerPictureManager.getInstance().hasPicture(packet.hash)) {
                    PicturePacketReceiver.getInstance().receivePacket(packet);
                }
            }
        });

        context.setPacketHandled(true);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(hash);
        buf.writeByteArray(data);
        buf.writeInt(index);
        buf.writeInt(totalPacket);
        buf.writeInt(totalLength);
        buf.writeVarInt(format.ordinal());
    }
}