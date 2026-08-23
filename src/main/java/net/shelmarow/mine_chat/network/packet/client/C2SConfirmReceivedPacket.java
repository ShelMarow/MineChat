package net.shelmarow.mine_chat.network.packet.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;

import java.util.function.Supplier;

public class C2SConfirmReceivedPacket {

    private final String hash;

    public C2SConfirmReceivedPacket(String hash) {
        this.hash = hash;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(hash);
    }

    public static C2SConfirmReceivedPacket decode(FriendlyByteBuf buf) {
        return new C2SConfirmReceivedPacket(buf.readUtf());
    }

    public static void handle(C2SConfirmReceivedPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                ServerPictureManager.getInstance().confirmReceived(packet.hash, player.getUUID());
            }
        });

        context.setPacketHandled(true);
    }

    public String getHash() {
        return hash;
    }
}