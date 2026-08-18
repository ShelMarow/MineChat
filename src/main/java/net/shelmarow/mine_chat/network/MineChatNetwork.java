package net.shelmarow.mine_chat.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.network.packet.client.C2SConfirmReceivedPacket;
import net.shelmarow.mine_chat.network.packet.client.C2SRequestPicturePacket;
import net.shelmarow.mine_chat.network.packet.client.C2SSendPicturePacket;
import net.shelmarow.mine_chat.network.packet.client.C2SServerInstallTestPacket;
import net.shelmarow.mine_chat.network.packet.server.S2CPictureRequestResultPacket;
import net.shelmarow.mine_chat.network.packet.server.S2CSendPicturePacket;
import net.shelmarow.mine_chat.network.packet.server.S2CServerInstalledPacket;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = MineChat.MOD_ID)
public class MineChatNetwork {

    private static final String NETWORK_VERSION = "1";
    private static final Set<UUID> AVAILABLE_CLIENTS = new HashSet<>();

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {

        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);

        /*
         * Client -> Server
         */
        registrar.optional().playToServer(C2SServerInstallTestPacket.TYPE, C2SServerInstallTestPacket.STREAM_CODEC, C2SServerInstallTestPacket::handle);
        registrar.optional().playToServer(C2SSendPicturePacket.TYPE, C2SSendPicturePacket.STREAM_CODEC, C2SSendPicturePacket::handle);
        registrar.optional().playToServer(C2SRequestPicturePacket.TYPE, C2SRequestPicturePacket.STREAM_CODEC, C2SRequestPicturePacket::handle);
        registrar.optional().playToServer(C2SConfirmReceivedPacket.TYPE, C2SConfirmReceivedPacket.STREAM_CODEC, C2SConfirmReceivedPacket::handle);

        /*
         * Server -> Client
         */
        registrar.optional().playToClient(S2CServerInstalledPacket.TYPE, S2CServerInstalledPacket.STREAM_CODEC, S2CServerInstalledPacket::handle);
        registrar.optional().playToClient(S2CSendPicturePacket.TYPE, S2CSendPicturePacket.STREAM_CODEC, S2CSendPicturePacket::handle);
        registrar.optional().playToClient(S2CPictureRequestResultPacket.TYPE, S2CPictureRequestResultPacket.STREAM_CODEC, S2CPictureRequestResultPacket::handle);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        AVAILABLE_CLIENTS.remove(event.getEntity().getUUID());
    }

    public static void add(UUID uuid) {
        AVAILABLE_CLIENTS.add(uuid);
    }

    public static void remove(UUID uuid) {
        AVAILABLE_CLIENTS.remove(uuid);
    }

    public static boolean isAvailable(UUID uuid) {
        return AVAILABLE_CLIENTS.contains(uuid);
    }

    public static void clear() {
        AVAILABLE_CLIENTS.clear();
    }

    public static  Set<UUID> getAvailableClients() {
        return AVAILABLE_CLIENTS;
    }
}
