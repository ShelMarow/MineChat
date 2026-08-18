package net.shelmarow.mine_chat.chat.npc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.ChatDataStorage;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class NPCDialogManager {
    private static final NPCDialogManager INSTANCE = new NPCDialogManager();

    //每个NPC正在执行的对话任务
    private static final Map<UUID, Deque<NPCDialog>> NPC_DIALOG_QUEST = new HashMap<>();

    private NPCDialogManager() {}

    public static NPCDialogManager getInstance() {
        return INSTANCE;
    }

    public void tryProcessDialog(ChatSender sender, LocalPlayer player) {
        UUID uuid = sender.getUuid();
        if(NPC_DIALOG_QUEST.containsKey(uuid)){
            Deque<NPCDialog> dialogs = NPC_DIALOG_QUEST.get(uuid);
            if(!dialogs.isEmpty()){
                NPCDialog dialog = dialogs.getFirst();
                dialog.process(player);
                Deque<NPCDialog> currentQueue = NPC_DIALOG_QUEST.get(uuid);
                if (dialog.isFinished()) {
                    currentQueue.pollFirst();
                    if (currentQueue.isEmpty()) {
                        NPC_DIALOG_QUEST.remove(uuid);
                        ChatDataStorage.saveNPCProgress();
                    }
                }
            }
        }
    }

    public void putNPCDialog(UUID uuid, NPCDialog dialog, boolean reset){
        if(reset){
            dialog.reset();
        }
        NPC_DIALOG_QUEST.computeIfAbsent(uuid, k -> new ArrayDeque<>()).add(dialog);
        tryProcessDialog(dialog.getChatSender(), Minecraft.getInstance().player);
        ChatDataStorage.saveNPCProgress();
        MineChatManager.setDMMessageCheckStatus(uuid, true);
    }

    public @Nullable NPCDialog getCurrentQuest(UUID uuid){
        if(NPC_DIALOG_QUEST.containsKey(uuid)){
            Deque<NPCDialog> dialogs = NPC_DIALOG_QUEST.get(uuid);
            if(!dialogs.isEmpty()){
                return dialogs.getFirst();
            }
        }
        return null;
    }

    public Map<UUID, Deque<NPCDialog>> getQuests(){
        return new HashMap<>(NPC_DIALOG_QUEST);
    }

    public void clearQuest() {
        NPC_DIALOG_QUEST.clear();
    }
}
