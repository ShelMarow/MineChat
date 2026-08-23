package net.shelmarow.mine_chat.chat.npc;

import net.minecraft.server.level.ServerPlayer;
import net.shelmarow.mine_chat.chat.storage.ServerDataStorage;

import java.util.*;

public class NPCDialogManager {
    private static final NPCDialogManager INSTANCE = new NPCDialogManager();

    //每个玩家的正在执行的NPC对话任务
    private final Map<UUID, Map<UUID, Deque<NPCDialog>>> NPC_DIALOG_QUEST = new HashMap<>();


    /**
     * 服务端保存玩家的NPC对话推进进度
     * 开始执行后发送首条消息给客户端，客户端自行推进等待时间
     * 客户端推进完毕并显示消息后，向服务端发送通信申请继续推进进度
     * 服务端接收到后重复行为直到所有对话都发送完毕
     * <p>
     * 玩家进入游戏时,服务端向玩家重新同步当前进度，防止数据丢失导致的遗漏
     */



    private NPCDialogManager() {}

    public static NPCDialogManager getInstance() {
        return INSTANCE;
    }


    //推进当前对话
    private void processDialog(UUID uuid, ServerPlayer serverPlayer) {
        Deque<NPCDialog> npcDialogs = NPC_DIALOG_QUEST.getOrDefault(serverPlayer.getUUID(), new HashMap<>()).get(uuid);
        if (npcDialogs != null && !npcDialogs.isEmpty()) {
            NPCDialog npcDialog = npcDialogs.getFirst();
            if(npcDialog != null) {
                //推进
                npcDialog.processAction(serverPlayer);
                //如果结束了就移除
                if(npcDialog.isFinished()){
                    npcDialogs.removeFirst();
                    processDialog(uuid, serverPlayer);
                }

                ServerDataStorage.saveNPCProgress(serverPlayer);
            }
        }
    }

    //收到客户端的结束回调后保存进度并继续推进
    public void actionFinished(UUID uuid, ServerPlayer serverPlayer) {
        Deque<NPCDialog> npcDialogs = NPC_DIALOG_QUEST.getOrDefault(serverPlayer.getUUID(), new HashMap<>()).get(uuid);
        if (npcDialogs != null && !npcDialogs.isEmpty()) {
            NPCDialog npcDialog = npcDialogs.getFirst();
            if(npcDialog != null) {
                npcDialog.actionFinished(serverPlayer);
                processDialog(uuid, serverPlayer);
            }
        }
    }


    public void addNPCDialogQuest(UUID uuid, ServerPlayer serverPlayer, NPCDialog dialog, boolean reset){
        if(reset){
            dialog.reset();
        }
        Deque<NPCDialog> npcDialogs = NPC_DIALOG_QUEST.computeIfAbsent(serverPlayer.getUUID(), k -> new HashMap<>())
                .computeIfAbsent(uuid, k -> new ArrayDeque<>());
        npcDialogs.add(dialog);

        //如果没有其他任务正在执行，那么立即执行一次当前任务
        if(npcDialogs.size() == 1){
            processDialog(uuid, serverPlayer);
        }
        ServerDataStorage.saveNPCProgress(serverPlayer);
    }

    public Map<UUID, Map<UUID, Deque<NPCDialog>>> getQuests(){
        return new HashMap<>(NPC_DIALOG_QUEST);
    }

    public void clearQuest(UUID uuid) {
        NPC_DIALOG_QUEST.remove(uuid);
    }
}
