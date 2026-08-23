package net.shelmarow.mine_chat;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.shelmarow.mine_chat.chat.npc.NPCDialogRegister;
import net.shelmarow.mine_chat.config.MineChatServerConfig;
import org.slf4j.Logger;

@Mod(MineChat.MOD_ID)
public class MineChat {
    public static final String MOD_ID = "mine_chat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MineChat(ModContainer container) {
        NPCDialogRegister.init();
        container.registerConfig(ModConfig.Type.SERVER, MineChatServerConfig.SERVER_CONFIG, "mine_chat_server.toml");
    }
}
