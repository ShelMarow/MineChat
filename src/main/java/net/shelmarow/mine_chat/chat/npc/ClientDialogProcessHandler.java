package net.shelmarow.mine_chat.chat.npc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.npc.action.DialogAction;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.NPCSenderManager;
import net.shelmarow.mine_chat.chat.storage.ClientChatDataStorage;
import net.shelmarow.mine_chat.network.packet.client.C2SActionFinishedPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClientDialogProcessHandler {

    private static final ClientDialogProcessHandler INSTANCE = new ClientDialogProcessHandler();

    private final Map<UUID, Deque<DialogActionProcesser>> NPC_DIALOG_QUEST = new HashMap<>();

    private int delayTick = 0;

    private ClientDialogProcessHandler() {}

    public static ClientDialogProcessHandler getInstance() {
        return INSTANCE;
    }

    public @Nullable DialogActionProcesser processDialogAction(ChatSender sender, LocalPlayer player) {
        UUID uuid = sender.getUuid();
        if(NPC_DIALOG_QUEST.containsKey(uuid)){
            Deque<DialogActionProcesser> dialogs = NPC_DIALOG_QUEST.getOrDefault(uuid, new ArrayDeque<>());
            if(!dialogs.isEmpty()){
                DialogActionProcesser actionProcesser = dialogs.getFirst();

                DialogAction action = actionProcesser.getAction();

                if(delayTick > 0){
                    delayTick--;
                }
                else {
                    //执行Action内容
                    action.execute(sender, player, actionProcesser.getTimer());
                    actionProcesser.addTime();
                }


                //没有选项的情况下到时间就标记结束
                //有选项的话需要等待按下按钮
                if(action.getOptions().isEmpty()){
                    actionProcesser.setFinished(actionProcesser.getTimer() > action.getWaitTime());
                }

                //完成时调用
                if(actionProcesser.isFinished()){
                    //执行客户端回调并移除
                    action.getCallback().accept(player);
                    dialogs.removeFirst();
                    delayNextAction(10);
                    PacketDistributor.sendToServer(new C2SActionFinishedPacket(sender.getUuid().toString()));
                    ClientChatDataStorage.saveNPCDM();
                }

                return actionProcesser;
            }
        }

        return null;
    }

    public void addDialogActionQuest(UUID target, DialogAction action, boolean first){
        DialogActionProcesser processer = new DialogActionProcesser(action);
        NPC_DIALOG_QUEST.computeIfAbsent(target, k-> new ArrayDeque<>()).add(processer);
        MineChatManager.setDMMessageCheckStatus(target, true);

        ChatSender npcData = NPCSenderManager.getInstance().getNpcData(target);
        if(npcData != null){
            if(MineChatManager.getNPCMessages(target).isEmpty() || first){
                delayTick = 0;
                processDialogAction(npcData, Minecraft.getInstance().player);
            }
        }
        ClientChatDataStorage.saveNPCDM();
    }

    public @Nullable DialogActionProcesser getCurrentAction(UUID uuid) {
        Deque<DialogActionProcesser> actionProcessers = NPC_DIALOG_QUEST.get(uuid);
        if(actionProcessers != null && !actionProcessers.isEmpty()){
            return actionProcessers.getFirst();
        }
        return null;
    }

    public void clearQuest(){
        NPC_DIALOG_QUEST.clear();
    }

    public void delayNextAction(int tick) {
        this.delayTick = tick;
    }

    public static class DialogActionProcesser{
        private final DialogAction action;
        private int timer = 0;
        private boolean finished = false;

        public DialogActionProcesser(@NotNull DialogAction action) {
            this.action = action;
        }

        public DialogAction getAction() {
            return action;
        }

        public int getTimer() {
            return timer;
        }

        public void addTime() {
            if(this.timer <= this.action.getWaitTime()){
                this.timer++;
            }
        }

        public void setFinished(boolean finished) {
            this.finished = finished;
        }

        public boolean isFinished() {
            return finished;
        }

        public boolean shouldDisplayOption(){
            return !action.getOptions().isEmpty() && timer >= action.getWaitTime();
        }

        public void setTimer(int timer) {
            this.timer = timer;
        }
    }
}
