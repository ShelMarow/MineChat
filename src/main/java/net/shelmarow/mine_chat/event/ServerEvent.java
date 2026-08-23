package net.shelmarow.mine_chat.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.npc.NPCDialogManager;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;
import net.shelmarow.mine_chat.chat.storage.ServerDataStorage;
import net.shelmarow.mine_chat.command.MineChatCommand;

@Mod.EventBusSubscriber(modid = MineChat.MOD_ID)
public class ServerEvent {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if(event.phase == TickEvent.Phase.START){
            ServerPictureManager.getInstance().tick();
        }
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
    public static void registerCommand(RegisterCommandsEvent event) {
        MineChatCommand.register(event.getDispatcher());
    }
}
