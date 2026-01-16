package net.shelmarow.mine_chat.chat.message.chat_enum;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum AnimationStatus {
    FADE_IN,
    STAY,
    FADE_OUT,
    FINISHED
}
