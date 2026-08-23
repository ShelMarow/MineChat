package net.shelmarow.mine_chat.chat.npc.action;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class DialogAction {
    protected int waitTime = 20;
    protected List<String> options = new ArrayList<>();
    protected Consumer<Player> callback = player -> {};

    public boolean canExecute(int timer){
        return timer <= waitTime;
    }

    @OnlyIn(Dist.CLIENT)
    public abstract void execute(@NotNull ChatSender chatSender, LocalPlayer player, int timer);

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public Consumer<Player> getCallback() {
        return callback;
    }

    public void setCallback(Consumer<Player> callback) {
        this.callback = callback;
    }
}
