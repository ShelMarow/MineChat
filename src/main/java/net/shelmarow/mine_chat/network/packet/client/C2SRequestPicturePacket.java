package net.shelmarow.mine_chat.network.packet.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;

import java.util.function.Supplier;

public class C2SRequestPicturePacket {

    private final String hash;

    public C2SRequestPicturePacket(String hash) {
        this.hash = hash;
    }

    public static C2SRequestPicturePacket decode(FriendlyByteBuf buf) {
        return new C2SRequestPicturePacket(buf.readUtf());
    }

    public static void handle(C2SRequestPicturePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                ServerPictureManager.getInstance().requestPictureToClient(packet.hash, player.getUUID());
            }
        });

        context.setPacketHandled(true);
    }

    public String getHash() {
        return hash;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(hash);
    }
}