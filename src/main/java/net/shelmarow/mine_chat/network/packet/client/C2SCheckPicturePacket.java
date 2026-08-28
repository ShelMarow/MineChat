package net.shelmarow.mine_chat.network.packet.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;
import net.shelmarow.mine_chat.config.MineChatServerConfig;
import net.shelmarow.mine_chat.network.packet.server.S2CCheckPictureSucceedPacket;
import org.jetbrains.annotations.NotNull;

public record C2SCheckPicturePacket(String hash) implements CustomPacketPayload {

    public static final Type<C2SCheckPicturePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "check_picture_packet"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SCheckPicturePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    C2SCheckPicturePacket::hash,
                    C2SCheckPicturePacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SCheckPicturePacket packet, IPayloadContext context) {
        context.enqueueWork(()->{
            if(MineChatServerConfig.ENABLE_NETWORK_PICTURE.get()){
                boolean hasPicture = ServerPictureManager.getInstance().hasPicture(packet.hash());
                if(!hasPicture){
                    if(context.player() instanceof ServerPlayer serverPlayer){
                        PacketDistributor.sendToPlayer(serverPlayer, new S2CCheckPictureSucceedPacket(packet.hash()));
                    }
                }
            }
        });
    }
}
