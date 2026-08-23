package net.shelmarow.mine_chat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.shelmarow.mine_chat.chat.npc.NPCDialogRegister;
import net.shelmarow.mine_chat.config.MineChatClientConfig;
import net.shelmarow.mine_chat.config.MineChatServerConfig;
import net.shelmarow.mine_chat.network.MineChatNetwork;
import org.slf4j.Logger;

@Mod(MineChat.MOD_ID)
public class MineChat {
    public static final String MOD_ID = "mine_chat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MineChat(FMLJavaModLoadingContext context) {
        NPCDialogRegister.init();

        MineChatNetwork.registerNetworkPacket();

        context.registerConfig(ModConfig.Type.SERVER, MineChatServerConfig.SERVER_CONFIG, "mine_chat_server.toml");
        if(FMLEnvironment.dist == Dist.CLIENT) {
            context.registerConfig(ModConfig.Type.CLIENT, MineChatClientConfig.CLIENT_CONFIG, "mine_chat_client.toml");
        }
    }
}
