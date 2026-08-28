package net.shelmarow.mine_chat.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.datapack.DialogReloadListener;
import net.shelmarow.mine_chat.chat.npc.NPCDialogManager;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;
import net.shelmarow.mine_chat.chat.storage.ServerDataStorage;
import net.shelmarow.mine_chat.command.MineChatCommand;
import net.shelmarow.mine_chat.config.MineChatServerConfig;
import net.shelmarow.mine_chat.network.packet.server.S2CServerInstalledPacket;
import net.shelmarow.mine_chat.network.packet.server.S2CSyncDatapackDialog;

@EventBusSubscriber(modid = MineChat.MOD_ID)
public class ServerEvent {

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new DialogReloadListener());
    }


    @SubscribeEvent
    public static void onDatapackSync(final OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            if(event.getPlayer().getServer() != null && !event.getPlayer().getServer().isSingleplayerOwner(event.getPlayer().getGameProfile())) {
                PacketDistributor.sendToPlayer(event.getPlayer(), new S2CSyncDatapackDialog(DialogReloadListener.getNpcDialogProviders()));
            }
        }
        else{
            event.getPlayerList().getPlayers().forEach(serverPlayer -> {
                PacketDistributor.sendToPlayer(serverPlayer, new S2CSyncDatapackDialog(DialogReloadListener.getNpcDialogProviders()));
            });
        }
    }


    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerPictureManager.getInstance().tick();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer serverPlayer){
            try{
                PacketDistributor.sendToPlayer(serverPlayer, new S2CServerInstalledPacket(MineChatServerConfig.ENABLE_NETWORK_PICTURE.get()));
            }
            catch (Exception e){
                serverPlayer.connection.disconnect(
                        Component.literal("MineChat client is required to join this server.")
                );
            }
            ServerDataStorage.loadNPCProgress(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if(event.getEntity() instanceof ServerPlayer serverPlayer){
            NPCDialogManager.getInstance().clearQuest(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void registerClientCommand(RegisterCommandsEvent event) {
        MineChatCommand.register(event.getDispatcher());
    }
}
