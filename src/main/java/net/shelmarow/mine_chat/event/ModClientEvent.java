package net.shelmarow.mine_chat.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.hud.MineChatHudRenderer;

@Mod.EventBusSubscriber(modid = MineChat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvent {

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("chat_hud", MineChatHudRenderer.instance);
    }
}
