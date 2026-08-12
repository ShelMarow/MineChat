package net.shelmarow.mine_chat.config;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

@OnlyIn(Dist.CLIENT)
public class MineChatConfig {
    public static final ForgeConfigSpec CLIENT_CONFIG;
    public static final ForgeConfigSpec.BooleanValue DISPLAY_RECENT_MESSAGES;
    public static final ForgeConfigSpec.DoubleValue RECENT_MESSAGES_SIZE;
    public static final ForgeConfigSpec.DoubleValue RECENT_MESSAGES_OFFSET_X;
    public static final ForgeConfigSpec.DoubleValue RECENT_MESSAGES_OFFSET_Y;
    public static final ForgeConfigSpec.IntValue MAX_DISPLAYED_MESSAGES;
    public static final ForgeConfigSpec.BooleanValue MIRRORED;
    public static final ForgeConfigSpec.EnumValue<AlignPos> ALIGN_POSITION;


    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("MineChat HUD Display Config");

        DISPLAY_RECENT_MESSAGES = builder.define("display_recent_messages",true);

        RECENT_MESSAGES_SIZE = builder.defineInRange("recent_messages_size", 1, 0, 10.0);

        RECENT_MESSAGES_OFFSET_X = builder.defineInRange("recent_messages_offset_x", 0, -Double.MAX_VALUE, Double.MAX_VALUE);

        RECENT_MESSAGES_OFFSET_Y = builder.defineInRange("recent_messages_offset_y", -60, -Double.MAX_VALUE, Double.MAX_VALUE);

        MAX_DISPLAYED_MESSAGES = builder.defineInRange("max_displayed_message", 3, 0, 20);

        MIRRORED = builder.define("mirrored", false);

        ALIGN_POSITION = builder.defineEnum("align_position", AlignPos.CENTER);

        builder.pop();

        CLIENT_CONFIG = builder.build();
    }

    public enum AlignPos{
        TOP,
        CENTER,
        BOTTOM;
    }
}
