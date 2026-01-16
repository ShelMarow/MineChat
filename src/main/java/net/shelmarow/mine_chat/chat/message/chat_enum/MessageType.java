package net.shelmarow.mine_chat.chat.message.chat_enum;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum MessageType {
    NOT_SHOWN,
    SYSTEM,
    OTHER,
    SAY,
    PLAYER_GLOBE,
    PLAYER_DM_IN,
    PLAYER_DM_OUT,
    PLAYER_TEAM_IN,
    PLAYER_TEAM_OUT
}
