package net.shelmarow.mine_chat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.shelmarow.mine_chat.chat.npc.NPCDialogRegister;
import net.shelmarow.mine_chat.config.MineChatConfig;

@Mod(value = MineChat.MOD_ID, dist = Dist.CLIENT)
public class MineChatClient {
    public MineChatClient(ModContainer container) {
        NPCDialogRegister.init();
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(ModConfig.Type.CLIENT, MineChatConfig.CLIENT_CONFIG, "mine_chat_client.toml");
    }
}
