package net.shelmarow.mine_chat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.shelmarow.mine_chat.config.MineChatClientConfig;

@Mod(value = MineChat.MOD_ID, dist = Dist.CLIENT)
public class MineChatClient {
    public MineChatClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(ModConfig.Type.CLIENT, MineChatClientConfig.CLIENT_CONFIG, "mine_chat_client.toml");
    }
}
