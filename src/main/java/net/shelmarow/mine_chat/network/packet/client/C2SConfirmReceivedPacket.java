package net.shelmarow.mine_chat.network.packet.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;
import org.jetbrains.annotations.NotNull;

public record C2SConfirmReceivedPacket(String hash) implements CustomPacketPayload {

    public static final Type<C2SConfirmReceivedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "confirm_picture_received"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SConfirmReceivedPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    C2SConfirmReceivedPacket::hash,
                    C2SConfirmReceivedPacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SConfirmReceivedPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerPictureManager.getInstance().confirmReceived(packet.hash(), player.getUUID());
            }
        });
    }
}
