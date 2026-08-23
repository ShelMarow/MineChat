package net.shelmarow.mine_chat.config;


import net.minecraftforge.common.ForgeConfigSpec;

public class MineChatServerConfig {
    public static final ForgeConfigSpec SERVER_CONFIG;

    public static final ForgeConfigSpec.BooleanValue ENABLE_NETWORK_PICTURE;


    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("display_settings");

        ENABLE_NETWORK_PICTURE = builder.define("enable_network_picture", true);

        builder.pop();

        SERVER_CONFIG = builder.build();
    }
}
