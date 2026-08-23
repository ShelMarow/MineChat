package net.shelmarow.mine_chat.chat.message;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.message.chat_enum.AnimationStatus;
import net.shelmarow.mine_chat.chat.message.chat_enum.MessageType;
import net.shelmarow.mine_chat.chat.sender.ChatSender;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public class AnimationMessage extends ChatMessage {
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w\\u4e00-\\u9fa5]+)");
    //消息进入的动画
    private final int maxRemainTime;
    private final List<MentionInfo> mentions = new ArrayList<>();
    private int fadeInTime;
    private int fadeOutTime;
    private int remainTime;
    private AnimationStatus animationStatus;
    //替换消息的高度变换动画
    private boolean changed = false;
    private int heightChangeTime;

    private boolean hasMention = false;
    private boolean mentionRead = false;

    public AnimationMessage(ChatSender sender, long timestamp, int nameLength, MessageType messageType, Component message) {
        this(sender, timestamp, nameLength, messageType, 230, 10, 20, message);
    }

    public AnimationMessage(ChatSender sender, long timestamp, int nameLength, MessageType messageType, int maxRemainTime, int fadeInTime, int fadeOutTime, Component message) {
        super(sender, timestamp, nameLength, messageType, message);
        this.maxRemainTime = maxRemainTime;
        this.fadeInTime = fadeInTime;
        this.fadeOutTime = fadeOutTime;
        this.animationStatus = AnimationStatus.FADE_IN;

        if (messageType == MessageType.PLAYER_GLOBE) {
            parseMentions(message);
        }
    }

    protected void parseMentions(Component message) {
        String rawText = message.getString();
        mentions.clear();
        hasMention = false;

        if (rawText.isEmpty()) {
            return;
        }

        Matcher matcher = MENTION_PATTERN.matcher(rawText);
        Minecraft mc = Minecraft.getInstance();

        while (matcher.find()) {
            String playerName = matcher.group(1);
            // 获取玩家 UUID
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if(connection != null) {
                PlayerInfo playerInfo = connection.getPlayerInfo(playerName);
                if(playerInfo != null) {
                    UUID targetUUID = playerInfo.getProfile().getId();
                    if (targetUUID != null) {
                        MentionInfo info = new MentionInfo(playerName, targetUUID, matcher.start(), matcher.end());
                        mentions.add(info);
                        // 检查是否提到了当前玩家
                        if (mc.player != null && targetUUID.equals(mc.player.getUUID())) {
                            hasMention = true;
                        }
                    }
                }
            }

        }

    }

    public void tick() {
        if (animationStatus != AnimationStatus.FINISHED) {
            remainTime++;
            if (remainTime <= fadeInTime) {
                animationStatus = AnimationStatus.FADE_IN;
            } else if (remainTime <= maxRemainTime - fadeOutTime) {
                animationStatus = AnimationStatus.STAY;
            } else if (remainTime > maxRemainTime - fadeOutTime && remainTime <= maxRemainTime) {
                animationStatus = AnimationStatus.FADE_OUT;
            } else {
                animationStatus = AnimationStatus.FINISHED;
            }
        }

        if (changed) {
            heightChangeTime++;
            if (heightChangeTime > maxRemainTime) {
                changed = false;
            }
        }
    }


    public void setChangeAnimation(boolean resetAnimation) {
        heightChangeTime = 0;
        changed = resetAnimation;
    }

    public float getChangedProgress(float partialTick) {
        if (!changed) {
            return 1;
        }
        return Mth.clamp((heightChangeTime + partialTick) / maxRemainTime, 0F, 1F);
    }

    public float getAnimationProgress(float partialTick) {
        if (isFinished()) {
            return 1F;
        }
        return Mth.clamp((remainTime + partialTick) / maxRemainTime, 0F, 1F);
    }

    public float getFadeInRatio(float partialTick) {
        return Mth.clamp((remainTime + partialTick) / fadeInTime, 0F, 1F);
    }

    public float getFadeOutRatio(float partialTick) {
        return Mth.clamp((maxRemainTime - (remainTime + partialTick)) / fadeOutTime, 0F, 1F);
    }

    public boolean isFinished() {
        return animationStatus == AnimationStatus.FINISHED && !changed;
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

    public record MentionInfo(String targetName, UUID targetUUID, int startIndex, int endIndex) {

        public String getMentionText() {
            return "@" + targetName;
        }

        public boolean isTargeting(UUID uuid) {
            return targetUUID.equals(uuid);
        }
    }

    public List<MentionInfo> getMentions() {
        return mentions;
    }

    public boolean isHasMention() {
        return hasMention;
    }

    public void setHasMention(boolean hasMention) {
        this.hasMention = hasMention;
    }

    public boolean isMentionRead() {
        return mentionRead;
    }

    public void setMentionRead(boolean mentionRead) {
        this.mentionRead = mentionRead;
    }
}
