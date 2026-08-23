package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;

import java.util.function.Supplier;

public class S2CPictureRequestResultPacket {

    private final String hash;
    private final boolean success;

    public S2CPictureRequestResultPacket(String hash, boolean success) {
        this.hash = hash;
        this.success = success;
    }

    public static S2CPictureRequestResultPacket decode(FriendlyByteBuf buf) {
        return new S2CPictureRequestResultPacket(buf.readUtf(), buf.readBoolean());
    }

    public static void handle(S2CPictureRequestResultPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            handleOnClient(packet);
        });

        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(S2CPictureRequestResultPacket packet) {
        ClientPictureManager.getInstance().handlePictureRequestResult(packet.hash, packet.success);
    }

    public String getHash() {
        return hash;
    }

    public boolean isSuccess() {
        return success;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(hash);
        buf.writeBoolean(success);
    }
}