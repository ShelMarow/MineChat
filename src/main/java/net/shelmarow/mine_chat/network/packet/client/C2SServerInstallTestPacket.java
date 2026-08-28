package net.shelmarow.mine_chat.network.packet.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.config.MineChatServerConfig;
import net.shelmarow.mine_chat.network.MineChatNetwork;
import net.shelmarow.mine_chat.network.packet.server.S2CServerInstalledPacket;
import org.jetbrains.annotations.NotNull;

public record C2SServerInstallTestPacket() implements CustomPacketPayload {

    public static final Type<C2SServerInstallTestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "server_install_test_packet"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SServerInstallTestPacket> STREAM_CODEC =
            StreamCodec.unit(new C2SServerInstallTestPacket());


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SServerInstallTestPacket packet, IPayloadContext context) {
        context.enqueueWork(()->{
            if(context.player() instanceof ServerPlayer serverPlayer){
                boolean serverEnabled = MineChatServerConfig.ENABLE_NETWORK_PICTURE.get();
                PacketDistributor.sendToPlayer(serverPlayer, new S2CServerInstalledPacket(serverEnabled));
            }
        });
    }
}
