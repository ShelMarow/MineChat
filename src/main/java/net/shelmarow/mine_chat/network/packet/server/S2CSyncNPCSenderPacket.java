package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.SenderType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record S2CSyncNPCSenderPacket(List<SenderData> senders) implements CustomPacketPayload {

    public static final Type<S2CSyncNPCSenderPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "sync_npc_sender"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncNPCSenderPacket> STREAM_CODEC =
            StreamCodec.of(
                    S2CSyncNPCSenderPacket::encode,
                    S2CSyncNPCSenderPacket::decode
            );

    private static void encode(RegistryFriendlyByteBuf buf, S2CSyncNPCSenderPacket packet) {
        List<SenderData> senders = packet.senders();
        buf.writeVarInt(senders.size());
        for (SenderData sender : senders) {
            buf.writeUUID(sender.uuid());


            buf.writeBoolean(sender.name() != null);
            if (sender.name() != null) {
                buf.writeUtf(sender.name());
            }

            buf.writeBoolean(sender.head() != null);
            if (sender.head() != null) {
                buf.writeResourceLocation(sender.head());
            }

            buf.writeBoolean(sender.customHead());
        }
    }


    private static S2CSyncNPCSenderPacket decode(RegistryFriendlyByteBuf buf) {

        int size = buf.readVarInt();

        List<SenderData> senders = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String name = null;
            boolean hasName = buf.readBoolean();
            if (hasName) {
                name = buf.readUtf();
            }

            ResourceLocation head = null;
            boolean hasHead = buf.readBoolean();
            if (hasHead) {
                head = buf.readResourceLocation();
            }

            boolean customHead = buf.readBoolean();
            senders.add(new SenderData(uuid, name, head, customHead));
        }

        return new S2CSyncNPCSenderPacket(senders);
    }


    public static void handle(S2CSyncNPCSenderPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            handleOnClient(packet.senders());
        });
    }


    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(List<SenderData> senders) {
        for (SenderData data : senders) {
            ChatSender sender = new ChatSender(data.uuid(), data.name(), data.head(), SenderType.NPC);
            sender.setCustomHead(data.customHead());
        }
    }


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public record SenderData(UUID uuid, String name, ResourceLocation head, boolean customHead) {
        public static S2CSyncNPCSenderPacket fromChatSenders(List<ChatSender> senders) {
            List<SenderData> dataList = new ArrayList<>();
            for (ChatSender sender : senders) {
                if (sender == null) {
                    continue;
                }
                dataList.add(new SenderData(sender.getUuid(), sender.getName(), sender.getHead(), sender.isCustomHead()));
            }
            return new S2CSyncNPCSenderPacket(dataList);
        }
    }
}