package net.shelmarow.mine_chat.network;

import net.shelmarow.mine_chat.chat.picture.data.NetworkPicture;
import net.shelmarow.mine_chat.network.packet.client.C2SSendPicturePacket;
import net.shelmarow.mine_chat.network.packet.server.S2CSendPicturePacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PicturePacketManager {

    public static final int MAX_PACKET_SIZE = 1000 * 1024;

    public static List<C2SSendPicturePacket> splitPictureC2S(@NotNull NetworkPicture picture){
        List<C2SSendPicturePacket> packets = new ArrayList<>();

        String pictureId = picture.getHash();
        byte[] data = picture.getImageData();
        int totalSize = data.length;
        int totalPackets = (int) Math.ceil((double) totalSize / MAX_PACKET_SIZE);

        for (int i = 0; i < totalPackets; i++) {
            int start = i * MAX_PACKET_SIZE;
            int end = Math.min(start + MAX_PACKET_SIZE, totalSize);
            byte[] chunk = new byte[end - start];
            System.arraycopy(data, start, chunk, 0, chunk.length);

            packets.add(new C2SSendPicturePacket(
                    pictureId, chunk, i, totalPackets, totalSize, picture.isGif()
            ));
        }
        return packets;
    }

    public static List<S2CSendPicturePacket> splitPictureS2C(@NotNull NetworkPicture picture){
        List<S2CSendPicturePacket> packets = new ArrayList<>();

        String pictureId = picture.getHash();
        byte[] data = picture.getImageData();
        int totalSize = data.length;
        int totalPackets = (int) Math.ceil((double) totalSize / MAX_PACKET_SIZE);

        for (int i = 0; i < totalPackets; i++) {
            int start = i * MAX_PACKET_SIZE;
            int end = Math.min(start + MAX_PACKET_SIZE, totalSize);
            byte[] chunk = new byte[end - start];
            System.arraycopy(data, start, chunk, 0, chunk.length);

            packets.add(new S2CSendPicturePacket(
                    pictureId, chunk, i, totalPackets, totalSize, picture.isGif()
            ));
        }
        return packets;
    }
}
