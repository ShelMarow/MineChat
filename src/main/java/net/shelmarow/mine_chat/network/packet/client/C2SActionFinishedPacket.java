package net.shelmarow.mine_chat.network.packet.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.mine_chat.chat.npc.NPCDialogManager;

import java.util.UUID;
import java.util.function.Supplier;

public class C2SActionFinishedPacket {

    private final String uuid;

    public C2SActionFinishedPacket(String uuid) {
        this.uuid = uuid;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(uuid);
    }

    public static C2SActionFinishedPacket decode(FriendlyByteBuf buf) {
        return new C2SActionFinishedPacket(buf.readUtf());
    }

    public static void handle(C2SActionFinishedPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = context.getSender();

            if (serverPlayer != null) {
                NPCDialogManager.getInstance().actionFinished(UUID.fromString(packet.uuid), serverPlayer);
            }
        });

        context.setPacketHandled(true);
    }

    public String getUuid() {
        return uuid;
    }
}