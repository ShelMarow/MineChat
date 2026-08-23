package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import org.jetbrains.annotations.NotNull;

public record S2CPictureRequestResultPacket(String hash, boolean success) implements CustomPacketPayload {

    public static final Type<S2CPictureRequestResultPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "picture_request_result")
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPictureRequestResultPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    S2CPictureRequestResultPacket::hash,
                    ByteBufCodecs.BOOL,
                    S2CPictureRequestResultPacket::success,
                    S2CPictureRequestResultPacket::new
            );


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handle(S2CPictureRequestResultPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            handleOnClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(S2CPictureRequestResultPacket packet) {
        ClientPictureManager.getInstance().handlePictureRequestResult(packet.hash(), packet.success());
    }
}
