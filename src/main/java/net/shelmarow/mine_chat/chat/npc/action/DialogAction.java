package net.shelmarow.mine_chat.chat.npc.action;

import net.minecraft.client.player.LocalPlayer;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class DialogAction {
    protected int waitTime = 20;
    protected List<String> options = new ArrayList<>();
    protected Consumer<LocalPlayer> callback = localPlayer -> {};

    public boolean canExecute(int timer){
        return timer <= waitTime;
    }

    public boolean execute(@NotNull ChatSender chatSender, LocalPlayer player, int timer){
        return timer >= waitTime && options.isEmpty();
    }

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

    public Consumer<LocalPlayer> getCallback() {
        return callback;
    }

    public void setCallback(Consumer<LocalPlayer> callback) {
        this.callback = callback;
    }
}
