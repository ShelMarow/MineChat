package net.shelmarow.mine_chat.chat.npc.action;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import org.jetbrains.annotations.NotNull;

public class SendMessageAction extends DialogAction {
    private Component message = Component.empty();

    @Override
    public boolean execute(@NotNull ChatSender chatSender, LocalPlayer player, int timer) {
        if(timer == 0){
            MineChatManager.sendNPCMessage(chatSender, message, player.level().getGameTime());
        }
        if(timer < waitTime){

            String[] loading = {
                    "●○○○○○",
                    "○●○○○○",
                    "○○●○○○",
                    "○○○●○○",
                    "○○○○●○",
                    "○○○○○●",
            };

            int tickCount = player.tickCount / 5 % loading.length;

            MineChatManager.modifyLatestNPCMessage(chatSender.getUuid(), Component.literal(loading[tickCount]), false);
        }
        else if(timer == waitTime){
            MineChatManager.modifyLatestNPCMessage(chatSender.getUuid(), message, true);
        }
        return super.execute(chatSender, player, timer);
    }

    public Component getMessage() {
        return message;
    }

    public void setMessage(Component message) {
        this.message = message;
    }
}
