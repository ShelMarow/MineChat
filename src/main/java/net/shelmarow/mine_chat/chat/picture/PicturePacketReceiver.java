package net.shelmarow.mine_chat.chat.picture;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.picture.data.NetworkPicture;
import net.shelmarow.mine_chat.network.MineChatNetwork;
import net.shelmarow.mine_chat.network.packet.client.C2SConfirmReceivedPacket;
import net.shelmarow.mine_chat.network.packet.client.C2SSendPicturePacket;
import net.shelmarow.mine_chat.network.packet.server.S2CSendPicturePacket;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PicturePacketReceiver {

    private static final PicturePacketReceiver INSTANCE = new PicturePacketReceiver();

    private static final Map<String, PictureReassembly> PENDING_PICTURES = new ConcurrentHashMap<>();

    private PicturePacketReceiver() {
    }

    public static PicturePacketReceiver getInstance() {
        return INSTANCE;
    }


    @OnlyIn(Dist.CLIENT)
    public void receivePacket(S2CSendPicturePacket packet) {
        NetworkPicture picture = handlePacket(packet.hash(), packet.index(), packet.totalPacket(), packet.totalLength(), packet.format(), packet.data());
        if (picture != null) {
            ClientPictureManager manager = ClientPictureManager.getInstance();
            manager.putNetworkPicture(picture);
            manager.saveToNetworkFile(picture);
            manager.removeRequested(picture.getHash());
            MineChatNetwork.sendToServer(new C2SConfirmReceivedPacket(picture.getHash()));
        }
    }


    public void receivePacket(C2SSendPicturePacket packet) {
        NetworkPicture picture = handlePacket(packet.hash(), packet.index(), packet.totalPacket(), packet.totalLength(), packet.format(), packet.data());
        if (picture != null) {
            ServerPictureManager.getInstance().storeNetworkPicture(picture.getHash(), picture);
        }
    }


    private NetworkPicture handlePacket(String hash, int index, int totalPackets, int totalLength, PictureFormat format, byte[] data) {
        PictureReassembly reassembly;
        if (index == 0) {
            PENDING_PICTURES.remove(hash);
            reassembly = new PictureReassembly(hash, totalPackets, totalLength, format);
            PENDING_PICTURES.put(hash, reassembly);
        }
        else {
            reassembly = PENDING_PICTURES.get(hash);
        }

        if (reassembly == null) {
            return null;
        }

        reassembly.addPacket(index, data);
        if (reassembly.finished()) {
            NetworkPicture picture = reassembly.toNetworkPicture();
            PENDING_PICTURES.remove(hash);
            return picture;
        }

        return null;
    }


    private static class PictureReassembly {

        private final String hash;
        private final int totalPackets;
        private final int totalLength;
        private final PictureFormat format;

        private final byte[][] packets;

        private int receivedCount = 0;

        private PictureReassembly(String hash, int totalPackets, int totalLength, PictureFormat format) {
            this.hash = hash;
            this.totalPackets = totalPackets;
            this.totalLength = totalLength;
            this.format = format;

            this.packets = new byte[totalPackets][];
        }


        public synchronized void addPacket(int index, byte[] data) {
            if (index < 0 || index >= totalPackets || packets[index] != null) {
                return;
            }

            packets[index] = data;
            receivedCount++;
        }

        public synchronized boolean finished() {
            return receivedCount >= totalPackets;
        }

        public synchronized @Nullable NetworkPicture toNetworkPicture() {

            if (!finished()) {
                return null;
            }

            byte[] data = new byte[totalLength];
            int offset = 0;

            for (int i = 0; i < totalPackets; i++) {
                byte[] packet = packets[i];
                if (packet == null) {
                    return null;
                }

                // 防止数据长度超过 totalLength
                if (offset + packet.length > totalLength) {
                    return null;
                }

                System.arraycopy(packet, 0, data, offset, packet.length);
                offset += packet.length;
            }

            // 所有数据长度必须完全匹配
            if (offset != totalLength) {
                return null;
            }

            return new NetworkPicture(hash, data, format);
        }
    }
}