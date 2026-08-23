package net.shelmarow.mine_chat.network.packet.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;
import net.shelmarow.mine_chat.network.MineChatNetwork;
import net.shelmarow.mine_chat.network.packet.server.S2CCheckPictureSucceedPacket;

import java.util.function.Supplier;

public class C2SCheckPicturePacket {

    private final String hash;

    public C2SCheckPicturePacket(String hash) {
        this.hash = hash;
    }


    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(hash);
    }


    public static C2SCheckPicturePacket decode(FriendlyByteBuf buf) {
        return new C2SCheckPicturePacket(buf.readUtf());
    }

    public static void handle(C2SCheckPicturePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            boolean hasPicture = ServerPictureManager.getInstance().hasPicture(packet.hash);

            if (!hasPicture) {
                ServerPlayer serverPlayer = context.getSender();

                if (serverPlayer != null) {
                    MineChatNetwork.sendToPlayer(serverPlayer, new S2CCheckPictureSucceedPacket(packet.hash));
                }
            }
        });

        context.setPacketHandled(true);
    }

    public String getHash() {
        return hash;
    }
}