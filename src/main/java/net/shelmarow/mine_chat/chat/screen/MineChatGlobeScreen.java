package net.shelmarow.mine_chat.chat.screen;

import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.message.AnimationMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MineChatGlobeScreen extends MineChatScreen {

    public MineChatGlobeScreen() {
        super();
        this.currentPage = CurrentPage.GLOBE;
    }

    @Override
    public @NotNull List<AnimationMessage> getChatMessages() {
        List<AnimationMessage> messages = MineChatManager.getGlobeMessages();
        Collections.reverse(messages);
        return messages;
    }

    @Override
    protected void onEditBoxEnterPressed(LocalPlayer player) {
        super.onEditBoxEnterPressed(player);
        if(currentPage == CurrentPage.GLOBE) {
            player.connection.sendChat(this.mainEditBox.getValue());
            this.mainEditBox.setValue("");
            resetScroll();
        }
    }
}
