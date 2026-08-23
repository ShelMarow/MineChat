package net.shelmarow.mine_chat.chat.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import net.shelmarow.mine_chat.chat.texture.MineChatTextures;
import net.shelmarow.mine_chat.config.MineChatClientConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MineChatHudRenderer implements IGuiOverlay {

    public static final MineChatHudRenderer instance = new MineChatHudRenderer();
    protected static final Minecraft MC = Minecraft.getInstance();

    private static final int RECENT_MESSAGE_WIDTH = 128;
    private static final int RECENT_MESSAGE_TEXT_WIDTH = 120;
    private static final int RECENT_MESSAGE_MAX_LINES = 2;
    private static final float RECENT_MESSAGE_MIN_TEXT_SCALE = 0.65F;

    private RecentMessageText createRecentMessageText(Font font, Component text) {

        int maxWidth = RECENT_MESSAGE_TEXT_WIDTH;
        int maxHeight = RECENT_MESSAGE_MAX_LINES * font.lineHeight;

        List<FormattedCharSequence> originalLines = new ArrayList<>(font.split(text, maxWidth));

        if (originalLines.size() <= RECENT_MESSAGE_MAX_LINES) {
            return new RecentMessageText(1.0F, originalLines, false);
        }


        float minScale = RECENT_MESSAGE_MIN_TEXT_SCALE;
        float maxScale = 1.0F;

        float bestScale = minScale;
        List<FormattedCharSequence> bestLines = null;

        final float EPSILON = 0.001F;

        while (maxScale - minScale > EPSILON) {

            float scale = (minScale + maxScale) * 0.5F;

            int scaledWidth = Math.max(1, Mth.floor(maxWidth / scale));

            int availableLines = Mth.floor(maxHeight / (font.lineHeight * scale));

            availableLines = Math.max(1, availableLines);

            List<FormattedCharSequence> lines = new ArrayList<>(font.split(text, scaledWidth));

            if (lines.size() <= availableLines) {
                bestScale = scale;
                bestLines = lines;
                minScale = scale;
            } else {
                maxScale = scale;
            }
        }


        float scale = bestScale;

        int scaledWidth;

        int availableLines = Mth.floor(maxHeight / (font.lineHeight * scale));

        availableLines = Math.max(1, availableLines);

        if (bestLines != null && bestLines.size() <= availableLines) {
            return new RecentMessageText(scale, bestLines, false);
        }


        scale = RECENT_MESSAGE_MIN_TEXT_SCALE;
        scaledWidth = Math.max(1, Mth.floor(maxWidth / scale));

        availableLines = Mth.floor(maxHeight / (font.lineHeight * scale));

        availableLines = Math.max(1, availableLines);

        List<FormattedCharSequence> scaledLines = new ArrayList<>(font.split(text, scaledWidth));

        if (scaledLines.size() <= availableLines) {
            return new RecentMessageText(scale, scaledLines, false);
        }


        List<FormattedCharSequence> result = new ArrayList<>();
        int lineCount = availableLines;

        for (int i = 0; i < lineCount; i++) {
            result.add(scaledLines.get(i));
        }


        int ellipsisWidth = font.width("...");

        int availableLastLineWidth = Math.max(1, scaledWidth - ellipsisWidth);

        FormattedCharSequence lastLine = result.get(result.size() - 1);


        StringBuilder lastLineBuilder = new StringBuilder();

        lastLine.accept((index, style, codePoint) -> {
            lastLineBuilder.appendCodePoint(codePoint);
            return true;
        });

        String lastLineText = lastLineBuilder.toString();

        String shortenedText = font.plainSubstrByWidth(lastLineText, availableLastLineWidth);

        String finalText = shortenedText + "...";

        FormattedCharSequence formattedFinalLine = font.split(Component.literal(finalText), scaledWidth).get(0);

        result.set(result.size() - 1, formattedFinalLine);

        return new RecentMessageText(scale, result, true);
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {

        if (MC.options.hideGui) {
            return;
        }

        boolean mirrored = MineChatClientConfig.MIRRORED.get();

        MineChatClientConfig.AlignPos alignPos = MineChatClientConfig.ALIGN_POSITION.get();

        Font font = MC.font;

        PoseStack poseStack = guiGraphics.pose();

        float offsetX = MineChatClientConfig.RECENT_MESSAGES_OFFSET_X.get().floatValue();

        float offsetY = MineChatClientConfig.RECENT_MESSAGES_OFFSET_Y.get().floatValue();

        float x = 4F + offsetX;
        float y = offsetY;

        switch (alignPos) {
            case TOP -> {
                y += 0;
            }
            case CENTER -> {
                y += guiGraphics.guiHeight() / 2F;
            }
            case BOTTOM -> {
                y += guiGraphics.guiHeight();
            }
        }

        if (mirrored) {
            x = guiGraphics.guiWidth() - 4F - offsetX - RECENT_MESSAGE_WIDTH;
        }


        poseStack.pushPose();
        poseStack.translate(x, y, 0);

        if (MineChatManager.hasUncheckedMessage()) {
            long millis = System.currentTimeMillis();
            poseStack.pushPose();
            poseStack.translate(mirrored ? 8 + 128 - 16 : 8, 16 + 8, 0);
            if (MineChatManager.shouldRotation()) {
                float rotationSpeed = 360F;
                float rotationAngle = (millis % 36000) * rotationSpeed / 36000F;
                poseStack.mulPose(Axis.ZP.rotation(rotationAngle));
            }

            guiGraphics.blit(MineChatTextures.CHAT_ICON_UNREAD, -8, -8, 0, 0, 16, 16, 16, 16);
            poseStack.popPose();

            float alpha;
            int i = 1500;
            int i1 = i / 2;
            long ratio = millis % i;
            if (ratio < i1) {
                alpha = Mth.clamp(1 - ((float) ratio / i1), 0.1F, 1);
            } else {
                alpha = Mth.clamp((float) (ratio - i1) / i1, 0.1F, 1);
            }

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, alpha);

            int lineOffset = 16 + 6;
            if (MineChatManager.isTeamChatUnchecked()) {
                MutableComponent translatable = Component.translatable("text.mine_chat.chat_unread_team");
                guiGraphics.drawString(font, translatable, mirrored ? 128 - 20 - font.width(translatable.getString()) : 20, lineOffset, 0xFFFFFF);
                lineOffset -= 9;
            }

            if (MineChatManager.isDMChatUnchecked() || MineChatManager.isNPChatUnchecked()) {
                MutableComponent translatable = Component.translatable("text.mine_chat.chat_unread_dm");
                guiGraphics.drawString(font, translatable, mirrored ? 128 - 20 - font.width(translatable.getString()) : 20, lineOffset, 0xFFFFFF);
                lineOffset -= 9;
            }

            if (MineChatManager.isPingUnchecked()) {
                MutableComponent translatable = Component.translatable("text.mine_chat.mention_you");
                guiGraphics.drawString(font, translatable, mirrored ? 128 - 20 - font.width(translatable.getString()) : 20, lineOffset, 0xFFFFFF);
            }

            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.disableBlend();

        } else {
            float alpha = 1 - MineChatManager.getIconDisplayRatio(partialTick);
            if (alpha > 0) {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1, 1, 1, alpha);

                guiGraphics.blit(MineChatTextures.CHAT_ICON, mirrored ? 128 - 16 : 0, 16, 0, 0, 16, 16, 16, 16);

                RenderSystem.setShaderColor(1, 1, 1, 1);
                RenderSystem.disableBlend();
            }
        }

        poseStack.popPose();

        if (!MineChatClientConfig.DISPLAY_RECENT_MESSAGES.get()) {
            return;
        }


        List<AnimationMessage> messages = MineChatManager.getLatestGlobeMessages();
        Collections.reverse(messages);

        float size = MineChatClientConfig.RECENT_MESSAGES_SIZE.get().floatValue();
        poseStack.pushPose();
        poseStack.translate(x, y + 32, 0);
        poseStack.scale(size, size, 1);

        float messageY = 0;

        for (AnimationMessage message : messages) {
            if (messageY > 0 && messageY + 35 > MC.getWindow().getGuiScaledHeight() * 0.8F) {
                break;
            }

            float fadeInRatio = 1;
            float fadeOutRatio = 1;

            if (message.getAnimationStatus() == AnimationStatus.FADE_IN) {
                fadeInRatio = message.getFadeInRatio(partialTick);
            }
            else if (message.getAnimationStatus() == AnimationStatus.FADE_OUT) {
                fadeOutRatio = message.getFadeOutRatio(partialTick);
            }

            poseStack.pushPose();

            if (mirrored) {
                poseStack.translate(128, messageY, 0);
                poseStack.scale(fadeInRatio, fadeInRatio, 1);
                poseStack.translate(-128, 0, 0);
            } else {
                poseStack.translate(0, messageY, 0);
                poseStack.scale(fadeInRatio, fadeInRatio, 1);
            }

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fadeOutRatio);

            guiGraphics.blit(MineChatTextures.CHAT_RECENT_MESSAGE, 0, 0, 0, 0, 128, 35, 128, 35);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();


            if (mirrored) {
                poseStack.translate(128, 0, 0);
            }

            List<Component> components = message.getMessage().toFlatList();
            MutableComponent name = Component.empty();
            MutableComponent text = Component.empty();

            for (int i = 0; i < components.size(); i++) {
                if (i < message.getNameLength()) {
                    name.append(components.get(i));
                } else {
                    text.append(components.get(i));
                }
            }


            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fadeOutRatio);

            ClientPictureManager pictureManager = ClientPictureManager.getInstance();
            boolean isPicture = pictureManager.isPicture(text.getString());

            guiGraphics.drawString(font, name, mirrored ? 2 - 128 : 2, 5, 0xFFFFFF);

            if(isPicture){
                text = Component.translatable("text.mine_chat.picture_message");
            }

            RecentMessageText recentMessageText = createRecentMessageText(font, text);
            int textX = mirrored ? -128 + 6 : 6;

            poseStack.pushPose();
            poseStack.scale(recentMessageText.scale, recentMessageText.scale, 1.0F);

            int scaledTextX = Mth.floor(textX / recentMessageText.scale);
            int scaledTextY = Mth.floor(14 / recentMessageText.scale);

            for (int i = 0; i < recentMessageText.lines.size(); i++) {
                guiGraphics.drawString(font, recentMessageText.lines.get(i), scaledTextX, scaledTextY + i * font.lineHeight, 0xFFFFFF);
            }

            poseStack.popPose();


            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();

            poseStack.popPose();

            messageY += fadeInRatio * 35;
        }

        poseStack.popPose();
    }


    private record RecentMessageText(float scale, List<FormattedCharSequence> lines, boolean truncated) {

    }
}