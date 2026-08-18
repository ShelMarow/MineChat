package net.shelmarow.mine_chat.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.ServerPictureManager;

@EventBusSubscriber(modid = MineChat.MOD_ID)
public class ServerEvent {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerPictureManager.getInstance().tick();
    }
}
