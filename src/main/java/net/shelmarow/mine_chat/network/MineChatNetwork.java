package net.shelmarow.mine_chat.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.network.packet.client.*;
import net.shelmarow.mine_chat.network.packet.server.*;

public class MineChatNetwork {

    private static final String NETWORK_VERSION = "1";

    private static int packetId = 0;

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "main"))
            .networkProtocolVersion(() -> NETWORK_VERSION)
            .clientAcceptedVersions(MineChatNetwork::clientAcceptedVersion)
            .serverAcceptedVersions(MineChatNetwork::serverAcceptedVersion)
            .simpleChannel();

    private static boolean clientAcceptedVersion(String version) {
        return NETWORK_VERSION.equals(version)
                || "ABSENT \uD83E\uDD14".equals(version)
                || "ACCEPTVANILLA".equals(version);
    }

    private static boolean serverAcceptedVersion(String version) {
        return NETWORK_VERSION.equals(version);
    }

    public static SimpleChannel getChannel() {
        return CHANNEL;
    }

    public static void sendToPlayer(ServerPlayer player, Object... packets) {
        for (Object packet : packets) {
            CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    packet
            );
        }
    }

    public static void sendToServer(Object... packets) {
        for (Object packet : packets) {
            CHANNEL.sendToServer(packet);
        }
    }

    public static void registerNetworkPacket() {

        CHANNEL.messageBuilder(
                        C2SServerInstallTestPacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(C2SServerInstallTestPacket::encode)
                .decoder(C2SServerInstallTestPacket::decode)
                .consumerMainThread(C2SServerInstallTestPacket::handle)
                .add();

        CHANNEL.messageBuilder(
                        C2SSendPicturePacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(C2SSendPicturePacket::encode)
                .decoder(C2SSendPicturePacket::decode)
                .consumerMainThread(C2SSendPicturePacket::handle)
                .add();

        CHANNEL.messageBuilder(
                        C2SRequestPicturePacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(C2SRequestPicturePacket::encode)
                .decoder(C2SRequestPicturePacket::decode)
                .consumerMainThread(C2SRequestPicturePacket::handle)
                .add();

        CHANNEL.messageBuilder(
                        C2SConfirmReceivedPacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(C2SConfirmReceivedPacket::encode)
                .decoder(C2SConfirmReceivedPacket::decode)
                .consumerMainThread(C2SConfirmReceivedPacket::handle)
                .add();

        CHANNEL.messageBuilder(
                        C2SActionFinishedPacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(C2SActionFinishedPacket::encode)
                .decoder(C2SActionFinishedPacket::decode)
                .consumerMainThread(C2SActionFinishedPacket::handle)
                .add();

        CHANNEL.messageBuilder(
                        C2SCheckPicturePacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(C2SCheckPicturePacket::encode)
                .decoder(C2SCheckPicturePacket::decode)
                .consumerMainThread(C2SCheckPicturePacket::handle)
                .add();


        CHANNEL.messageBuilder(
                        S2CServerInstalledPacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(S2CServerInstalledPacket::encode)
                .decoder(S2CServerInstalledPacket::decode)
                .consumerMainThread(S2CServerInstalledPacket::handle)
                .add();

        CHANNEL.messageBuilder(
                        S2CSendPicturePacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(S2CSendPicturePacket::encode)
                .decoder(S2CSendPicturePacket::decode)
                .consumerMainThread(S2CSendPicturePacket::handle)
                .add();

        CHANNEL.messageBuilder(
                        S2CPictureRequestResultPacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(S2CPictureRequestResultPacket::encode)
                .decoder(S2CPictureRequestResultPacket::decode)
                .consumerMainThread(S2CPictureRequestResultPacket::handle)
                .add();

        CHANNEL.messageBuilder(
                        S2CSendActionPacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(S2CSendActionPacket::encode)
                .decoder(S2CSendActionPacket::decode)
                .consumerMainThread(S2CSendActionPacket::handle)
                .add();

        CHANNEL.messageBuilder(
                        S2CCheckPictureSucceedPacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(S2CCheckPictureSucceedPacket::encode)
                .decoder(S2CCheckPictureSucceedPacket::decode)
                .consumerMainThread(S2CCheckPictureSucceedPacket::handle)
                .add();

        CHANNEL.messageBuilder(
                        S2CSyncDatapackDialog.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(S2CSyncDatapackDialog::encode)
                .decoder(S2CSyncDatapackDialog::decode)
                .consumerMainThread(S2CSyncDatapackDialog::handle)
                .add();

        CHANNEL.messageBuilder(
                        S2CSyncNPCSenderPacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(S2CSyncNPCSenderPacket::encode)
                .decoder(S2CSyncNPCSenderPacket::decode)
                .consumerMainThread(S2CSyncNPCSenderPacket::handle)
                .add();
    }
}