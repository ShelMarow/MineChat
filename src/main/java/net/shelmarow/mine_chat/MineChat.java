package net.shelmarow.mine_chat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.level.block.BedBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.shelmarow.mine_chat.chat.sound.MineChatSounds;
import net.shelmarow.mine_chat.config.MineChatConfig;
import org.slf4j.Logger;

@Mod(MineChat.MOD_ID)
public class MineChat {
    public static final String MOD_ID = "mine_chat";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MineChat(FMLJavaModLoadingContext context) {
        if(FMLEnvironment.dist == Dist.CLIENT) {
            context.registerConfig(ModConfig.Type.CLIENT, MineChatConfig.CLIENT_CONFIG, "mine_chat_client.toml");
        }
    }
}
