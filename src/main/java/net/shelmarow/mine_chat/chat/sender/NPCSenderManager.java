package net.shelmarow.mine_chat.chat.sender;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCSenderManager {

    private static final NPCSenderManager INSTANCE = new NPCSenderManager();

    //保存的NPC数据索引缓存
    private final Map<UUID, ChatSender> NPC_DATA = new HashMap<>();

    private NPCSenderManager() {}

    public static NPCSenderManager getInstance() {
        return INSTANCE;
    }

    public @Nullable ChatSender getNpcData(UUID uuid){
        return NPC_DATA.get(uuid);
    }

    public void cacheNPC(ChatSender chatSender) {
        NPC_DATA.put(chatSender.getUuid(), chatSender);
    }

}
