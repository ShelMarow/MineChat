package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import net.shelmarow.mine_chat.chat.picture.PicturePacketManager;
import net.shelmarow.mine_chat.chat.picture.data.NetworkPicture;
import net.shelmarow.mine_chat.network.MineChatNetwork;
import net.shelmarow.mine_chat.network.packet.client.C2SSendPicturePacket;

import java.util.List;
import java.util.function.Supplier;

public class S2CCheckPictureSucceedPacket {

    private final String hash;

    public S2CCheckPictureSucceedPacket(String hash) {
        this.hash = hash;
    }

    public static S2CCheckPictureSucceedPacket decode(FriendlyByteBuf buf) {
        return new S2CCheckPictureSucceedPacket(buf.readUtf());
    }

    public static void handle(S2CCheckPictureSucceedPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            handleOnClient(packet);
        });

        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(S2CCheckPictureSucceedPacket packet) {
        NetworkPicture networkData = ClientPictureManager.getInstance().getNetworkData().get(packet.hash);

        if (networkData != null) {
            List<C2SSendPicturePacket> packets = PicturePacketManager.splitPictureC2S(networkData);

            for (C2SSendPicturePacket packetData : packets) {
                MineChatNetwork.sendToServer(packetData);
            }
        }
    }

    public String getHash() {
        return hash;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(hash);
    }
}