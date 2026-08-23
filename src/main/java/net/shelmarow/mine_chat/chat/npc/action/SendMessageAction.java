package net.shelmarow.mine_chat.chat.npc.action;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class SendMessageAction extends DialogAction {
    private Function<Player, Component> messageFun = player-> Component.empty();
    private Component message = Component.empty();

    @OnlyIn(Dist.CLIENT)
    @Override
    public void execute(@NotNull ChatSender chatSender, LocalPlayer player, int timer) {

        if(timer == 0){
            this.message = messageFun.apply(player);
            setWaitTime(calculateWaitTime(message));
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
    }


    public void setMessage(Function<Player, Component> messageFun) {
        this.messageFun = messageFun;
    }

    private int calculateWaitTime(Component message){

        String text = message.getString();

        if(text.isEmpty()){
            return 10;
        }

        int weight = 0;
        for(char c : text.toCharArray()){
            if(c >= 0x4E00 && c <= 0x9FFF){
                weight += 2;
            }
            else{
                weight += 1;
            }
        }

        int time = (weight / 30) * 20;
        return Mth.clamp(time, 10, 40);
    }
}
