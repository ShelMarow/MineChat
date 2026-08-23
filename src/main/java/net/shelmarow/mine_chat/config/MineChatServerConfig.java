package net.shelmarow.mine_chat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MineChatServerConfig {
    public static final ModConfigSpec SERVER_CONFIG;

    public static final ModConfigSpec.BooleanValue ENABLE_NETWORK_PICTURE;


    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("display_settings");

        ENABLE_NETWORK_PICTURE = builder.define("enable_network_picture", true);

        builder.pop();

        SERVER_CONFIG = builder.build();
    }
}
