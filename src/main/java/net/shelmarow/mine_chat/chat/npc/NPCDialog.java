package net.shelmarow.mine_chat.chat.npc;

import com.google.gson.JsonObject;
import net.minecraft.Util;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shelmarow.mine_chat.chat.ChatDataStorage;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.npc.action.DialogAction;
import net.shelmarow.mine_chat.chat.npc.action.SendMessageAction;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.SenderType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class NPCDialog {
    private String dialogID = "";
    private @NotNull ChatSender chatSender = new ChatSender(Util.NIL_UUID, null, null, SenderType.SYSTEM);
    private int executedIndex = -1;
    private int currentIndex = 0;
    private int timer = 0;
    private boolean canContinue = false;
    private boolean finished = false;
    private List<DialogAction> actions = new ArrayList<>();

    public void reset() {
        this.executedIndex = -1;
        this.currentIndex = 0;
        this.timer = 0;
        this.canContinue = false;
        this.finished = false;
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("dialogID", dialogID);
        jsonObject.addProperty("sender", chatSender.getUuid().toString());
        jsonObject.addProperty("executedIndex", executedIndex);
        jsonObject.addProperty("currentIndex", currentIndex);
        jsonObject.addProperty("canContinue", canContinue);
        jsonObject.addProperty("finished", finished);
        jsonObject.addProperty("timer", timer);
        return jsonObject;
    }

    public void fromJson(JsonObject json) {
        this.dialogID = json.get("dialogID").getAsString();
        ChatSender npcData = MineChatManager.getNpcData(UUID.fromString(json.get("sender").getAsString()));
        if(npcData != null) {
            this.chatSender = npcData;
        }
        this.executedIndex = json.get("executedIndex").getAsInt();
        this.currentIndex = json.get("currentIndex").getAsInt();
        this.canContinue = json.get("canContinue").getAsBoolean();
        this.finished = json.get("finished").getAsBoolean();
        this.timer = json.get("timer").getAsInt();
    }

    public void process(LocalPlayer player) {
        if (canProcess()) {
            DialogAction dialogAction = actions.get(currentIndex);
            if (timer >= 0) {
                if(dialogAction.canExecute(timer)) {
                    canContinue = dialogAction.execute(chatSender, player, timer);
                    ChatDataStorage.saveNPCProgress();
                    if(timer == dialogAction.getWaitTime()){
                        dialogAction.getCallback().accept(player);
                    }
                }
                if(executedIndex != currentIndex){
                    executedIndex = currentIndex;
                    ChatDataStorage.saveNPCProgress();
                }
            }
            timer++;
            if (canContinue) {
                canContinue = false;
                currentIndex++;
                timer = -10;
                ChatDataStorage.saveNPCProgress();
            }
        } else {
            finished = true;
            ChatDataStorage.saveNPCProgress();
        }
    }

    public boolean canProcess() {
        return !actions.isEmpty() && !finished && currentIndex >= 0 && currentIndex < actions.size();
    }

    public boolean canExecute(DialogAction dialogAction) {
        return timer >= dialogAction.getWaitTime();
    }


    public boolean canExecute() {
        DialogAction dialogAction = actions.get(currentIndex);
        return timer >= dialogAction.getWaitTime();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public boolean isCanContinue() {
        return canContinue;
    }

    public void setCanContinue(boolean canContinue) {
        this.canContinue = canContinue;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public List<DialogAction> getActions() {
        return actions;
    }

    public void setActions(List<DialogAction> actions) {
        this.actions = actions;
    }

    public boolean haveOptions() {
        if (!actions.isEmpty() && !finished && currentIndex >= 0 && currentIndex < actions.size()) {
            return !actions.get(currentIndex).getOptions().isEmpty();
        }
        return false;
    }

    public List<String> getCurrentOptions() {
        if (!actions.isEmpty() && !finished && currentIndex >= 0 && currentIndex < actions.size()) {
            return actions.get(currentIndex).getOptions();
        }
        return new ArrayList<>();
    }

    public String getDialogID() {
        return dialogID;
    }

    public void setDialogID(String dialogID) {
        this.dialogID = dialogID;
    }

    private void addAction(SendMessageAction action) {
        actions.add(action);
    }

    public @NotNull ChatSender getChatSender() {
        return chatSender;
    }

    public void setChatSender(@NotNull ChatSender chatSender) {
        this.chatSender = chatSender;
    }

    public void setTimer(int timer) {
        this.timer = timer;
    }

    public int getTimer() {
        return timer;
    }

    public int getExecutedIndex() {
        return executedIndex;
    }

    public static class Builder {

        private static final int MIN_WAIT_TIME = 10;
        private static final int MAX_WAIT_TIME = 40;

        private final String dialogID;
        private final ChatSender chatSender;
        private final List<DialogAction> actions = new ArrayList<>();


        public Builder(String dialogID, @NotNull ChatSender chatSender) {
            this.dialogID = dialogID;
            this.chatSender = chatSender;
        }


        public Builder sendMessage(Component message){
            return sendMessage(message, new ArrayList<>(), localPlayer -> {});
        }


        public Builder sendMessage(Component message, List<String> options){
            return sendMessage(message, options, localPlayer -> {});
        }


        public Builder sendMessage(Component message, List<String> options, Consumer<LocalPlayer> callback){
            SendMessageAction action = new SendMessageAction();
            action.setWaitTime(calculateWaitTime(message));
            action.setMessage(message);
            action.setOptions(options);
            action.setCallback(callback);
            actions.add(action);
            return this;
        }

        public Builder sendPicture(ResourceLocation pictureId, boolean isSystem){
            return sendPicture(pictureId, isSystem, p->{});
        }

        public Builder sendPicture(ResourceLocation pictureId, boolean isSystem, Consumer<LocalPlayer> callback){
            SendMessageAction action = new SendMessageAction();
            action.setWaitTime(10);
            action.setMessage(Component.literal("<MineChatPicture:[\"" + pictureId.toString() + "|" + (isSystem ? "system" : "chat") + "\"]>"));
            action.setCallback(callback);
            actions.add(action);
            return this;
        }


        private int calculateWaitTime(Component message){

            String text = message.getString();

            if(text.isEmpty()){
                return MIN_WAIT_TIME;
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
            return Mth.clamp(time, MIN_WAIT_TIME, MAX_WAIT_TIME);
        }


        private void register() {
            NPCDialogRegister.registerNPCDialog(dialogID, this::createDialog);
        }


        private NPCDialog createDialog() {

            NPCDialog dialog = new NPCDialog();

            dialog.setDialogID(dialogID);
            dialog.setActions(actions);
            dialog.setChatSender(chatSender);

            return dialog;
        }


        public NPCDialog build() {
            NPCDialog dialog = createDialog();
            register();
            return dialog;
        }
    }
}
