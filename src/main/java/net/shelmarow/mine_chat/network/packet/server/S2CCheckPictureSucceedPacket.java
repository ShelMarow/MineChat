package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import net.shelmarow.mine_chat.chat.picture.PicturePacketManager;
import net.shelmarow.mine_chat.chat.picture.data.NetworkPicture;
import net.shelmarow.mine_chat.network.packet.client.C2SSendPicturePacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record S2CCheckPictureSucceedPacket(String hash) implements CustomPacketPayload {

    public static final Type<S2CCheckPictureSucceedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "check_picture_succeed"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CCheckPictureSucceedPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    S2CCheckPictureSucceedPacket::hash,
                    S2CCheckPictureSucceedPacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CCheckPictureSucceedPacket packet, IPayloadContext context) {
        context.enqueueWork(()->{
            handleOnClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(S2CCheckPictureSucceedPacket msg){
        NetworkPicture networkData = ClientPictureManager.getInstance().getNetworkData().get(msg.hash);
        if (networkData != null) {
            List<C2SSendPicturePacket> packets = PicturePacketManager.splitPictureC2S(networkData);
            for (C2SSendPicturePacket packet : packets) {
                PacketDistributor.sendToServer(packet);
            }
        }
    }
}
