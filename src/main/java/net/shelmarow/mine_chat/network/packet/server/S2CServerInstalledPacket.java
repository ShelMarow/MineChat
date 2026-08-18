package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import org.jetbrains.annotations.NotNull;

public record S2CServerInstalledPacket(boolean serverInstalled) implements CustomPacketPayload {

    public static final Type<S2CServerInstalledPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "server_installed_packet"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CServerInstalledPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    S2CServerInstalledPacket::serverInstalled,
                    S2CServerInstalledPacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void handle(S2CServerInstalledPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            handleOnClient(packet, context.player());
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(S2CServerInstalledPacket packet, Player player) {
        ClientPictureManager.getInstance().setServerInstalled(packet.serverInstalled);
        if (packet.serverInstalled) {
            player.displayClientMessage(Component.translatable("text.mine_chat.server_installed"),false);
        }
    }
}
