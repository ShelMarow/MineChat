package net.shelmarow.mine_chat.chat;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.mine_chat.MineChat;

import java.util.ArrayDeque;
import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = MineChat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MineChatManager {
    private static final int MAX_SIZE = 100;
    private static final ArrayDeque<Component> CHAT_MESSAGE_QUEUE = new ArrayDeque<>();

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent event) {
        Component message = event.getMessage();
        add(message);
    }

    public static void add(Component msg) {
        if (CHAT_MESSAGE_QUEUE.size() >= MAX_SIZE) {
            CHAT_MESSAGE_QUEUE.pollFirst();
        }
        CHAT_MESSAGE_QUEUE.addLast(msg);
    }

    public static List<Component> getLatestMessages(int count) {
        if (count <= 0) return List.of();
        return CHAT_MESSAGE_QUEUE.stream().skip(Math.max(0, CHAT_MESSAGE_QUEUE.size() - count)).collect(Collectors.toList());
    }

    public static Component getLatestMessage() {
        List<Component> latestMessages = getLatestMessages(1);
        return latestMessages.isEmpty() ? Component.empty() : latestMessages.get(0);
    }

}
