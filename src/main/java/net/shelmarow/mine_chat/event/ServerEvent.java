package net.shelmarow.mine_chat.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.datapack.DialogReloadListener;
import net.shelmarow.mine_chat.chat.datapack.NPCSenderReloadListener;
import net.shelmarow.mine_chat.chat.npc.NPCDialogManager;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;
import net.shelmarow.mine_chat.chat.storage.ServerDataStorage;
import net.shelmarow.mine_chat.command.MineChatCommand;
import net.shelmarow.mine_chat.config.MineChatServerConfig;
import net.shelmarow.mine_chat.network.MineChatNetwork;
import net.shelmarow.mine_chat.network.packet.server.S2CServerInstalledPacket;
import net.shelmarow.mine_chat.network.packet.server.S2CSyncDatapackDialog;
import net.shelmarow.mine_chat.network.packet.server.S2CSyncNPCSenderPacket;

@Mod.EventBusSubscriber(modid = MineChat.MOD_ID)
public class ServerEvent {



    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new NPCSenderReloadListener());
        event.addListener(new DialogReloadListener());
    }


    @SubscribeEvent
    public static void onDatapackSync(final OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            if(event.getPlayer().getServer() != null && !event.getPlayer().getServer().isSingleplayerOwner(event.getPlayer().getGameProfile())) {
                MineChatNetwork.sendToPlayer(event.getPlayer(), new S2CSyncDatapackDialog(DialogReloadListener.getNpcDialogProviders()));
                MineChatNetwork.sendToPlayer(event.getPlayer(), S2CSyncNPCSenderPacket.SenderData.fromChatSenders(NPCSenderReloadListener.getSenders()));
            }
        }
        else{
            event.getPlayerList().getPlayers().forEach(serverPlayer -> {
                MineChatNetwork.sendToPlayer(serverPlayer, new S2CSyncDatapackDialog(DialogReloadListener.getNpcDialogProviders()));
                MineChatNetwork.sendToPlayer(serverPlayer, S2CSyncNPCSenderPacket.SenderData.fromChatSenders(NPCSenderReloadListener.getSenders()));
            });
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if(event.phase == TickEvent.Phase.START){
            ServerPictureManager.getInstance().tick();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer serverPlayer){
            try{
               MineChatNetwork.sendToPlayer(serverPlayer, new S2CServerInstalledPacket(MineChatServerConfig.ENABLE_NETWORK_PICTURE.get()));
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
    public static void registerCommand(RegisterCommandsEvent event) {
        MineChatCommand.register(event.getDispatcher());
    }
}
