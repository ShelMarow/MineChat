package net.shelmarow.mine_chat.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.npc.NPCDialogManager;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;
import net.shelmarow.mine_chat.chat.storage.ServerDataStorage;
import net.shelmarow.mine_chat.command.MineChatCommand;

@EventBusSubscriber(modid = MineChat.MOD_ID)
public class ServerEvent {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerPictureManager.getInstance().tick();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof ServerPlayer serverPlayer){
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
