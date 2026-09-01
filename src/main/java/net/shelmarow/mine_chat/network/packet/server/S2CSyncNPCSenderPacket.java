package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.SenderType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class S2CSyncNPCSenderPacket {
    private final List<SenderData> senders;

    public S2CSyncNPCSenderPacket(List<SenderData> senders) {
        this.senders = senders;
    }

    private S2CSyncNPCSenderPacket(List<SenderData> senders, boolean ignored) {
        this.senders = senders;
    }

    public static void encode(S2CSyncNPCSenderPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.senders.size());

        for (SenderData sender : packet.senders) {
            buf.writeUUID(sender.uuid);
            buf.writeBoolean(sender.name != null);

            if (sender.name != null) {
                buf.writeUtf(sender.name);
            }

            buf.writeBoolean(sender.head != null);

            if (sender.head != null) {
                buf.writeResourceLocation(sender.head);
            }

            buf.writeBoolean(sender.customHead);
        }
    }

    public static S2CSyncNPCSenderPacket decode(FriendlyByteBuf buf) {
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
        return new S2CSyncNPCSenderPacket(senders, true);
    }

    public static void handle(S2CSyncNPCSenderPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            handleOnClient(packet.senders);
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(List<SenderData> senders) {
        for (SenderData data : senders) {
            ChatSender sender = new ChatSender(data.uuid, data.name, data.head, SenderType.NPC);
            sender.setCustomHead(data.customHead);
        }
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