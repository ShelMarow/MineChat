package net.shelmarow.mine_chat.chat.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.message.AnimationMessage;
import net.shelmarow.mine_chat.chat.message.chat_enum.AnimationStatus;
import net.shelmarow.mine_chat.chat.texture.MineChatTextures;
import net.shelmarow.mine_chat.config.MineChatConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MineChatHudRenderer implements IGuiOverlay {
    public static final MineChatHudRenderer instance = new MineChatHudRenderer();

    @Override
    public void render(ForgeGui forgeGui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {

        Font font = forgeGui.getFont();
        PoseStack poseStack = guiGraphics.pose();
        int x = 4;
        int y = screenHeight / 4;

        poseStack.pushPose();
        poseStack.translate(x, y, 0);

        //消息图标
        if(MineChatManager.hasUncheckedMessage()){

            float alpha;

            int i = 1500;
            int i1 = i/2;

            long ratio = System.currentTimeMillis() % i;

            if(ratio < i1){
                alpha = Mth.clamp(1 - ((float) ratio / i1), 0.1F, 1);
            }
            else{
                alpha = Mth.clamp((float) (ratio - i1) / i1, 0.1F, 1);
            }

            guiGraphics.blit(MineChatTextures.CHAT_ICON_UNREAD, 0, 16, 0, 0, 16, 16, 16, 16);

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, alpha);
            int lineOffset = 22;
            if(MineChatManager.isTeamChatUnchecked()) {
                guiGraphics.drawString(font, Component.translatable("text.mine_chat.chat_unread_team"), 20, lineOffset, 0xFFFFFF);
                lineOffset -= 9;
            }
            if(MineChatManager.isDMChatUnchecked()) {
                guiGraphics.drawString(font, Component.translatable("text.mine_chat.chat_unread_dm"), 20, lineOffset, 0xFFFFFF);
                lineOffset -= 9;
            }

            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
        else {
            guiGraphics.blit(MineChatTextures.CHAT_ICON, 0, 16, 0, 0, 16, 16, 16, 16);
        }

        poseStack.popPose();


        if(!MineChatConfig.DISPLAY_RECENT_MESSAGES.get()) return;

        //最新消息
        List<AnimationMessage> messages = MineChatManager.getLatestGlobeMessages();
        Collections.reverse(messages);

        float size = MineChatConfig.RECENT_MESSAGES_SIZE.get().floatValue();

        poseStack.pushPose();
        poseStack.translate(x, y + 32,0);
        poseStack.scale(size,size,1);

        float offsetY = 0;
        //循环渲染所有消息
        for (AnimationMessage message : messages) {

            if(offsetY > 0 && offsetY + 35 > forgeGui.getMinecraft().getWindow().getGuiScaledHeight() * 0.33F){
                break;
            }

            float fadeInRatio = 1;
            float fadeOutRatio = 1;

            if(message.getAnimationStatus() == AnimationStatus.FADE_IN) {
                fadeInRatio = message.getFadeInRatio(partialTick);
            }
            else if(message.getAnimationStatus() == AnimationStatus.FADE_OUT) {
                fadeOutRatio = message.getFadeOutRatio(partialTick);
            }

            poseStack.pushPose();
            poseStack.translate(0, offsetY,0);
            poseStack.scale(fadeInRatio, fadeInRatio, 1);

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fadeOutRatio);

            //消息背景图
            guiGraphics.blit(MineChatTextures.CHAT_RECENT_MESSAGE, 0, 0, 0, 0, 128, 35, 128, 35);
            //文本
            List<Component> components = message.getMessage().toFlatList();
            MutableComponent name = Component.empty();
            MutableComponent text = Component.empty();
            for (int i = 0; i < components.size(); i++) {
                if(i < message.getNameLength()){
                    name.append(components.get(i));
                }
                else{
                    text.append(components.get(i));
                }
            }


            guiGraphics.drawString(font, name, 2, 5, 0xFFFFFF);
            List<FormattedCharSequence> lines = new ArrayList<>(font.split(text, 120 - font.width("...")));
            for (int i = 0; i < lines.size(); i++) {
                guiGraphics.drawString(font, lines.get(i), 6, 5 + (i + 1) * font.lineHeight, 0xFFFFFF);
                if(i == 1 && lines.size() > 2){
                    guiGraphics.drawString(font, "...", 6 + font.width(lines.get(i)), 5 + (i + 1) * font.lineHeight, 0xFFFFFF);
                    break;
                }
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();

            poseStack.popPose();

            //添加偏移量
            offsetY += fadeInRatio * 35;
        }

        poseStack.popPose();

    }
}
