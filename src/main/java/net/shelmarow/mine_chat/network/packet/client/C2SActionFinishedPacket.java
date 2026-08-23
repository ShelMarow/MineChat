package net.shelmarow.mine_chat.network.packet.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.npc.NPCDialogManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record C2SActionFinishedPacket(String uuid) implements CustomPacketPayload {



    public static final Type<C2SActionFinishedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "action_finished_packet"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SActionFinishedPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    C2SActionFinishedPacket::uuid,
                    C2SActionFinishedPacket::new
            );


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SActionFinishedPacket packet, IPayloadContext context) {
        context.enqueueWork(()->{
            if(context.player() instanceof ServerPlayer serverPlayer){
                NPCDialogManager.getInstance().actionFinished(UUID.fromString(packet.uuid()), serverPlayer);
            }
        });
    }
}
