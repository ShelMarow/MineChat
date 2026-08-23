package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.mine_chat.chat.MineChatManager;

import java.util.function.Supplier;

public class S2CServerInstalledPacket {

    private final boolean serverInstalled;

    public S2CServerInstalledPacket(boolean serverInstalled) {
        this.serverInstalled = serverInstalled;
    }

    public static S2CServerInstalledPacket decode(FriendlyByteBuf buf) {
        return new S2CServerInstalledPacket(buf.readBoolean());
    }

    public static void handle(S2CServerInstalledPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            handleOnClient(packet);
        });

        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(S2CServerInstalledPacket packet) {
        MineChatManager.setServerInstalled(packet.serverInstalled);
    }

    public boolean isServerInstalled() {
        return serverInstalled;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(serverInstalled);
    }
}