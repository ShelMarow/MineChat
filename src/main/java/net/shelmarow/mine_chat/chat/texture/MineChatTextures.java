package net.shelmarow.mine_chat.chat.texture;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.MineChat;

@OnlyIn(Dist.CLIENT)
public class MineChatTextures {

    //HUD图标
    public static final ResourceLocation CHAT_ICON = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/chat.png");
    public static final ResourceLocation CHAT_ICON_UNREAD = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/chat_unread.png");
    public static final ResourceLocation CHAT_RECENT_MESSAGE = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/recent_message.png");

    //GUI界面材质
    public static final ResourceLocation RED_POINT = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/red_point.png");
    public static final ResourceLocation UNKNOW = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/unknown.png");;
    public static final ResourceLocation SYSTEM_ICON = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/system_icon.png");
    public static final ResourceLocation PLAYER_FRAME = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/player_frame.png");
    public static final ResourceLocation COMMON_CHANNEL = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/common_channel.png");
    public static final ResourceLocation DM_CHANNEL = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/dm_channel.png");
}
