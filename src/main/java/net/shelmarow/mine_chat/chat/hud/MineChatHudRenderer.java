package net.shelmarow.mine_chat.chat.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.message.AnimationMessage;
import net.shelmarow.mine_chat.chat.message.chat_enum.AnimationStatus;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import net.shelmarow.mine_chat.chat.texture.MineChatTextures;
import net.shelmarow.mine_chat.config.MineChatClientConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MineChatHudRenderer implements LayeredDraw.Layer {

    public static final MineChatHudRenderer instance = new MineChatHudRenderer();
    protected static final Minecraft MC = Minecraft.getInstance();

    private static final int RECENT_MESSAGE_WIDTH = 128;
    private static final int RECENT_MESSAGE_TEXT_WIDTH = 120;
    private static final int RECENT_MESSAGE_MAX_LINES = 2;
    private static final float RECENT_MESSAGE_MIN_TEXT_SCALE = 0.65F;

    private RecentMessageText createRecentMessageText(Font font, Component text) {

        int maxWidth = RECENT_MESSAGE_TEXT_WIDTH;
        int maxHeight = RECENT_MESSAGE_MAX_LINES * font.lineHeight;

        /*
         * ============================================================
         * 1. 先尝试 1 倍缩放
         * ============================================================
         */
        List<FormattedCharSequence> originalLines =
                new ArrayList<>(font.split(text, maxWidth));

        if (originalLines.size() <= RECENT_MESSAGE_MAX_LINES) {
            return new RecentMessageText(1.0F, originalLines, false);
        }

        /*
         * ============================================================
         * 2. 寻找能够完整容纳文本的最大缩放倍率
         *
         * scale 越大：
         *   - 每行能够容纳的原始字符越少
         *   - 字体高度越大
         *   - 能容纳的总行数越少
         *
         * scale 越小：
         *   - 每行能够容纳的原始字符越多
         *   - 字体高度越小
         *   - 能容纳的总行数越多
         *
         * 因此这是一个单调问题，可以使用二分查找。
         * ============================================================
         */

        float minScale = RECENT_MESSAGE_MIN_TEXT_SCALE;
        float maxScale = 1.0F;

        /*
         * 保存一个已经确认可以完整显示的倍率。
         *
         * 最小倍率后面如果也放不下，则会使用最小倍率进行裁剪。
         */
        float bestScale = minScale;
        List<FormattedCharSequence> bestLines = null;

        /*
         * 二分查找精度。
         *
         * 0.001F 对于 UI 缩放已经足够。
         */
        final float EPSILON = 0.001F;

        while (maxScale - minScale > EPSILON) {

            float scale = (minScale + maxScale) * 0.5F;

            /*
             * 当前缩放倍率下：
             *
             * 120 像素的实际显示宽度，
             * 相当于原始字体可以使用：
             *
             * 120 / scale
             *
             * 的宽度。
             */
            int scaledWidth = Math.max(
                    1,
                    Mth.floor(maxWidth / scale)
            );

            /*
             * 缩放后字体高度也会变小，
             * 因此能够容纳更多行。
             */
            int availableLines = Mth.floor(
                    maxHeight / (font.lineHeight * scale)
            );

            availableLines = Math.max(1, availableLines);

            /*
             * 按当前倍率的实际单行宽度重新换行。
             */
            List<FormattedCharSequence> lines =
                    new ArrayList<>(font.split(text, scaledWidth));

            /*
             * 如果完整文本可以放下：
             *
             * 当前倍率是可行的。
             *
             * 尝试提高倍率，让文字尽可能大。
             */
            if (lines.size() <= availableLines) {

                bestScale = scale;
                bestLines = lines;

                minScale = scale;

            } else {

                /*
                 * 当前倍率放不下，
                 * 必须继续缩小。
                 */
                maxScale = scale;
            }
        }

        /*
         * ============================================================
         * 3. 二分结束后，再检查最小倍率
         * ============================================================
         */

        float scale = bestScale;

        int scaledWidth = Math.max(
                1,
                Mth.floor(maxWidth / scale)
        );

        int availableLines = Mth.floor(
                maxHeight / (font.lineHeight * scale)
        );

        availableLines = Math.max(1, availableLines);

        /*
         * 如果之前已经找到完整显示的结果，
         * 直接使用。
         */
        if (bestLines != null && bestLines.size() <= availableLines) {
            return new RecentMessageText(
                    scale,
                    bestLines,
                    false
            );
        }

        /*
         * ============================================================
         * 4. 最小倍率依然无法完整显示
         *
         * 此时必须使用最小倍率 + ...
         * ============================================================
         */

        scale = RECENT_MESSAGE_MIN_TEXT_SCALE;

        scaledWidth = Math.max(
                1,
                Mth.floor(maxWidth / scale)
        );

        availableLines = Mth.floor(
                maxHeight / (font.lineHeight * scale)
        );

        availableLines = Math.max(1, availableLines);

        List<FormattedCharSequence> scaledLines =
                new ArrayList<>(font.split(text, scaledWidth));

        /*
         * 如果最小倍率实际上已经能够完整显示，
         * 直接返回。
         */
        if (scaledLines.size() <= availableLines) {
            return new RecentMessageText(
                    scale,
                    scaledLines,
                    false
            );
        }

        /*
         * ============================================================
         * 5. 最小倍率也无法显示
         *
         * 只保留能够显示的行数。
         * ============================================================
         */

        List<FormattedCharSequence> result = new ArrayList<>();

        int lineCount = Math.min(
                availableLines,
                scaledLines.size()
        );

        for (int i = 0; i < lineCount; i++) {
            result.add(scaledLines.get(i));
        }

        /*
         * ============================================================
         * 6. 给最后一行预留 "..."
         * ============================================================
         */

        int ellipsisWidth = font.width("...");

        int availableLastLineWidth = Math.max(
                1,
                scaledWidth - ellipsisWidth
        );

        FormattedCharSequence lastLine = result.getLast();

        /*
         * FormattedCharSequence -> String
         */
        StringBuilder lastLineBuilder = new StringBuilder();

        lastLine.accept((index, style, codePoint) -> {
            lastLineBuilder.appendCodePoint(codePoint);
            return true;
        });

        String lastLineText = lastLineBuilder.toString();

        /*
         * 裁剪最后一行，使其能够留出 "..." 的空间。
         */
        String shortenedText = font.plainSubstrByWidth(
                lastLineText,
                availableLastLineWidth
        );

        String finalText = shortenedText + "...";

        FormattedCharSequence formattedFinalLine =
                font.split(
                        Component.literal(finalText),
                        scaledWidth
                ).getFirst();

        result.set(
                result.size() - 1,
                formattedFinalLine
        );

        return new RecentMessageText(
                scale,
                result,
                true
        );
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {

        if (MC.options.hideGui) {
            return;
        }

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);

        boolean mirrored = MineChatClientConfig.MIRRORED.getAsBoolean();

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