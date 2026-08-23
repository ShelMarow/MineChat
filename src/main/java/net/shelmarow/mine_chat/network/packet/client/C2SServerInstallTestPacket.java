package net.shelmarow.mine_chat.network.packet.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.shelmarow.mine_chat.config.MineChatServerConfig;
import net.shelmarow.mine_chat.network.MineChatNetwork;
import net.shelmarow.mine_chat.network.packet.server.S2CServerInstalledPacket;

import java.util.function.Supplier;

public class C2SServerInstallTestPacket {

    public C2SServerInstallTestPacket() {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static C2SServerInstallTestPacket decode(FriendlyByteBuf buf) {
        return new C2SServerInstallTestPacket();
    }

    public static void handle(C2SServerInstallTestPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = context.getSender();

            if (serverPlayer != null) {
                boolean serverEnabled = MineChatServerConfig.ENABLE_NETWORK_PICTURE.get();
                MineChatNetwork.sendToPlayer(serverPlayer, new S2CServerInstalledPacket(serverEnabled));
            }
        });

        context.setPacketHandled(true);
    }
}