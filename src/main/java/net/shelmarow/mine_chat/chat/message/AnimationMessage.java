package net.shelmarow.mine_chat.chat.message;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.message.chat_enum.AnimationStatus;
import net.shelmarow.mine_chat.chat.message.chat_enum.MessageType;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class AnimationMessage extends ChatMessage{
    private final int maxRemainTime;
    private int fadeInTime;
    private int fadeOutTime;
    private int remainTime;
    private AnimationStatus animationStatus;

    public AnimationMessage(UUID sender, long timestamp, int nameLength, MessageType messageType, Component message) {
        this(sender, timestamp, nameLength, messageType, 230, 10, 20, message);
    }

    public AnimationMessage(UUID sender, long timestamp, int nameLength, MessageType messageType, int maxRemainTime, int fadeInTime, int fadeOutTime, Component message) {
        super(sender, timestamp, nameLength, messageType, message);
        this.maxRemainTime = maxRemainTime;
        this.fadeInTime = fadeInTime;
        this.fadeOutTime = fadeOutTime;
        this.animationStatus = AnimationStatus.FADE_IN;
    }

    public void tick(){
        if(animationStatus != AnimationStatus.FINISHED){
            remainTime++;
            if(remainTime <= fadeInTime){
                animationStatus = AnimationStatus.FADE_IN;
            }
            else if(remainTime <= maxRemainTime - fadeOutTime){
                animationStatus = AnimationStatus.STAY;
            }
            else if(remainTime > maxRemainTime - fadeOutTime && remainTime <= maxRemainTime){
                animationStatus = AnimationStatus.FADE_OUT;
            }
            else{
                animationStatus = AnimationStatus.FINISHED;
            }
        }
    }

    public float getAnimationProgress(float partialTick) {
        if(isFinished()){
            return 1F;
        }
        return Mth.clamp((remainTime + partialTick) / maxRemainTime, 0F, 1F);
    }

    public float getFadeInRatio(float partialTick){
        return Mth.clamp((remainTime + partialTick) / fadeInTime, 0F, 1F);
    }

    public float getFadeOutRatio(float partialTick) {
        return Mth.clamp((maxRemainTime - (remainTime + partialTick)) / fadeOutTime, 0F, 1F);
    }

    public boolean isFinished(){
        return animationStatus == AnimationStatus.FINISHED;
    }

    public int getMaxRemainTime() {
        return maxRemainTime;
    }

    public AnimationStatus getAnimationStatus() {
        return animationStatus;
    }

    public void setAnimationStatus(AnimationStatus animationStatus) {
        this.animationStatus = animationStatus;
    }

    public int getRemainTime() {
        return remainTime;
    }

    public void setRemainTime(int remainTime) {
        this.remainTime = remainTime;
    }

    public int getFadeInTime() {
        return fadeInTime;
    }

    public void setFadeInTime(int fadeInTime) {
        this.fadeInTime = fadeInTime;
    }

    public int getFadeOutTime() {
        return fadeOutTime;
    }

    public void setFadeOutTime(int fadeOutTime) {
        this.fadeOutTime = fadeOutTime;
    }
}
