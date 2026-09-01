package net.shelmarow.mine_chat.chat.npc;

import com.google.gson.JsonObject;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.shelmarow.mine_chat.chat.npc.action.DialogAction;
import net.shelmarow.mine_chat.chat.npc.action.SendMessageAction;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.NPCSenderManager;
import net.shelmarow.mine_chat.chat.sender.SenderType;
import net.shelmarow.mine_chat.network.MineChatNetwork;
import net.shelmarow.mine_chat.network.packet.server.S2CSendActionPacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NPCDialog {
    private String dialogID = "";
    private @NotNull ChatSender chatSender = new ChatSender(Util.NIL_UUID, null, null, SenderType.SYSTEM);
    private int currentIndex = 0;
    private boolean finished = false;
    private boolean forceOpenScreen = false;
    private List<Supplier<DialogAction>> actions = new ArrayList<>();

    public void reset() {
        this.finished = false;
        this.currentIndex = 0;
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("dialogID", dialogID);
        jsonObject.addProperty("sender", chatSender.getUuid().toString());
        jsonObject.addProperty("currentIndex", currentIndex);
        jsonObject.addProperty("finished", finished);
        jsonObject.addProperty("forceOpenScreen", forceOpenScreen);
        return jsonObject;
    }

    public void fromJson(JsonObject json) {
        this.dialogID = json.get("dialogID").getAsString();
        ChatSender npcData = NPCSenderManager.getInstance().getNpcData(UUID.fromString(json.get("sender").getAsString()));
        if(npcData != null) {
            this.chatSender = npcData;
        }
        this.currentIndex = json.get("currentIndex").getAsInt();
        this.finished = json.get("finished").getAsBoolean();
        this.forceOpenScreen = json.get("forceOpenScreen").getAsBoolean();
    }

    //发送Action给客户端执行
    public void processAction(ServerPlayer player) {
        if (canProcess()) {
            MineChatNetwork.sendToPlayer(player, new S2CSendActionPacket(dialogID, currentIndex, currentIndex == 0, forceOpenScreen));
        }
        else {
            finished = true;
        }
    }


    public void actionFinished(ServerPlayer player) {
        if (canProcess()) {
            DialogAction dialogAction = actions.get(currentIndex).get();
            dialogAction.getCallback().accept(player);
            currentIndex++;
        }
    }

    public boolean canProcess() {
        return !actions.isEmpty() && !finished && currentIndex >= 0 && currentIndex < actions.size();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public List<Supplier<DialogAction>> getActions() {
        return actions;
    }

    public void setActions(List<Supplier<DialogAction>> actions) {
        this.actions = actions;
    }

    public boolean haveOptions() {
        if (!actions.isEmpty() && !finished && currentIndex >= 0 && currentIndex < actions.size()) {
            return !actions.get(currentIndex).get().getOptions().isEmpty();
        }
        return false;
    }

    public List<String> getCurrentOptions() {
        if (!actions.isEmpty() && !finished && currentIndex >= 0 && currentIndex < actions.size()) {
            return actions.get(currentIndex).get().getOptions();
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
        actions.add(()-> action);
    }

    public @NotNull ChatSender getChatSender() {
        return chatSender;
    }

    public void setChatSender(@NotNull ChatSender chatSender) {
        this.chatSender = chatSender;
    }

    public boolean isForceOpenScreen() {
        return forceOpenScreen;
    }

    public void setForceOpenScreen(boolean forceOpenScreen) {
        this.forceOpenScreen = forceOpenScreen;
    }

    public static class Builder {

        private static final int MIN_WAIT_TIME = 10;
        private static final int MAX_WAIT_TIME = 40;

        private final String dialogID;
        private final ChatSender chatSender;
        private final boolean forceOpenScreen;
        private final List<Supplier<DialogAction>> actions = new ArrayList<>();


        public Builder(String dialogID, @NotNull ChatSender chatSender) {
            this.dialogID = dialogID;
            this.chatSender = chatSender;
            this.forceOpenScreen = false;
        }


        public Builder(String dialogID, @NotNull ChatSender chatSender, boolean forceOpenScreen) {
            this.dialogID = dialogID;
            this.chatSender = chatSender;
            this.forceOpenScreen = forceOpenScreen;
        }

        public Builder sendMessage(Component message){
            return sendMessage(message, new ArrayList<>(), localPlayer -> {});
        }


        public Builder sendMessage(Component message, List<String> options){
            return sendMessage(message, options, localPlayer -> {});
        }


        public Builder sendMessage(Component message, List<String> options, Consumer<Player> callback){
            actions.add(()->{
                SendMessageAction action = new SendMessageAction();
                action.setWaitTime(calculateWaitTime(message));
                action.setMessage(p-> message);
                action.setOptions(options);
                action.setCallback(callback);
                return action;
            });
            return this;
        }

        public Builder sendMessage(Function<Player, Component> messageFun){
            return this.sendMessage(messageFun, List.of(), localPlayer -> {});
        }

        public Builder sendMessage(Function<Player, Component> messageFun, List<String> options){
            return this.sendMessage(messageFun, options, localPlayer -> {});
        }

        public Builder sendMessage(Function<Player, Component> messageFun, List<String> options, Consumer<Player> callback){
            actions.add(()->{
                SendMessageAction action = new SendMessageAction();
                action.setMessage(messageFun);
                action.setOptions(options);
                action.setCallback(callback);
                return action;
            });
            return this;
        }

        public Builder sendPicture(ResourceLocation pictureId, boolean isSystem){
            return sendPicture(pictureId, isSystem, p->{});
        }

        public Builder sendPicture(ResourceLocation pictureId, boolean isSystem, Consumer<Player> callback){
            actions.add(()-> {
                SendMessageAction action = new SendMessageAction();
                action.setWaitTime(10);
                action.setMessage(p-> Component.literal("<MineChatPicture:[\"" + pictureId.toString() + "|" + (isSystem ? "system" : "chat") + "\"]>"));
                action.setCallback(callback);
                return action;
            });
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


        private NPCDialog createDialog() {

            NPCDialog dialog = new NPCDialog();

            dialog.setDialogID(dialogID);
            dialog.setActions(actions);
            dialog.setChatSender(chatSender);
            dialog.setForceOpenScreen(forceOpenScreen);

            return dialog;
        }


        public NPCDialog build() {
            NPCDialog dialog = createDialog();
            NPCDialogRegister.registerNPCDialog(dialogID, this::createDialog);
            return dialog;
        }

        public NPCDialog buildDatapack() {
            NPCDialog dialog = createDialog();
            NPCDialogRegister.registerDatapackDialog(dialogID, this::createDialog);
            return dialog;
        }
    }
}
