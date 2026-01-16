package net.shelmarow.mine_chat.config;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

@OnlyIn(Dist.CLIENT)
public class MineChatConfig {
    public static final ForgeConfigSpec CLIENT_CONFIG;
    public static final ForgeConfigSpec.BooleanValue DISPLAY_RECENT_MESSAGES;
    public static final ForgeConfigSpec.DoubleValue RECENT_MESSAGES_SIZE;


    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("MineChat HUD Display Config");

        DISPLAY_RECENT_MESSAGES = builder.define("display_recent_messages",true);

        RECENT_MESSAGES_SIZE = builder.defineInRange("recent_messages_size", 1, 0, 10.0);

        builder.pop();

        CLIENT_CONFIG = builder.build();
    }
}
