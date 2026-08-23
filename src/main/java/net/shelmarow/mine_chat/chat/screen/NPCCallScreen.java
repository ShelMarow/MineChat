package net.shelmarow.mine_chat.chat.screen;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.texture.MineChatTextures;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class NPCCallScreen extends Screen {

    private static final int HEAD_SIZE = 22;
    private static final int HEAD_INNER_SIZE = 16;

    private static final float HEAD_SCALE = 1.5F;
    private static final float NAME_SCALE = 1.5F;

    private static final float CALLING_SCALE = 1.0F;

    private static final int HEAD_START_OFFSET_X = 0;
    private static final int HEAD_FINAL_OFFSET_X = -45;

    private static final int HEAD_FADE_IN_TICKS = 5;
    private static final int INITIAL_HOLD_TICKS = 5;

    private static final int ENTER_TICKS = 10;
    private static final int HOLD_TICKS = 40;


    private static final int FINAL_NAME_ANIMATION_TICKS = 12;
    private static final int FINAL_NAME_OFFSET_X = 20;
    private static final int FINAL_NAME_START_OFFSET_X = -25;

    private static final float UNDERLINE_WIDTH_RATIO = 0.70F;
    private static final int UNDERLINE_OFFSET_Y = 12;
    private static final int UNDERLINE_HEIGHT = 2;
    private static final int UNDERLINE_ANIMATION_TICKS = 15;

    private static final int CALLING_OFFSET_Y = 8;
    private static final int CALLING_FADE_TICKS = 10;

    private static final int RING_START_TICKS = 0;
    private static final int RING_DURATION_TICKS = 18;

    private static final float RING_START_SCALE = 1.0F;
    private static final float RING_END_SCALE = 2.8F;
    private static final int RING_START_ALPHA = 100;
    private static final int RING_END_ALPHA = 0;

    private static final int WHITE = 0xFFFFFF;

    private final ChatSender sender;
    private int animationTick = 0;
    private float screenPartialTick = 0;

    public NPCCallScreen(ChatSender npc) {
        super(Component.empty());
        this.sender = npc;
    }

    private static float easeOutCubic(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float easeOutQuad(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return 1.0F - (1.0F - value) * (1.0F - value);
    }

    @Override
    protected void init() {
        super.init();
        animationTick = 0;
        screenPartialTick = 0;

        Window guiWindow = Minecraft.getInstance().getWindow();
        int screenWidth = guiWindow.getScreenWidth();
        int screenHeight = guiWindow.getScreenHeight();

        long window = guiWindow.getWindow();
        GLFW.glfwSetCursorPos(window, screenWidth, screenHeight);
    }

    @Override
    public void tick() {
        super.tick();
        screenPartialTick = 0;
        animationTick++;

        int initialPhase = HEAD_FADE_IN_TICKS + INITIAL_HOLD_TICKS;
        int totalAnimationTicks = initialPhase + ENTER_TICKS + HOLD_TICKS;

        if (animationTick >= totalAnimationTicks) {
            //animationTick = 0;
            Minecraft.getInstance().setScreen(new MineChatDMScreen(sender.getUuid()));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        screenPartialTick += partialTick;
        float time = animationTick + screenPartialTick;
        renderCall(guiGraphics, time);
    }

    private void renderCall(GuiGraphics guiGraphics, float time) {
        ResourceLocation frame = MineChatTextures.PLAYER_FRAME;
        ResourceLocation head = sender.getHead();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int initialPhase = HEAD_FADE_IN_TICKS + INITIAL_HOLD_TICKS;
        float moveTime = time - initialPhase;
        float moveProgress;
        if (moveTime <= 0.0F) {
            moveProgress = 0.0F;
        } else {
            moveProgress = Mth.clamp(moveTime / ENTER_TICKS, 0.0F, 1.0F);
        }
        moveProgress = easeOutCubic(moveProgress);

        int baseHeadX = centerX - HEAD_SIZE / 2;
        int baseHeadY = centerY - HEAD_SIZE / 2;
        int headX = (int) Mth.lerp(moveProgress, baseHeadX + HEAD_START_OFFSET_X, baseHeadX + HEAD_FINAL_OFFSET_X);

        float headFadeProgress = Mth.clamp(time / HEAD_FADE_IN_TICKS, 0.0F, 1.0F);
        headFadeProgress = easeOutCubic(headFadeProgress);

        float ringCenterX = headX + HEAD_SIZE * HEAD_SCALE / 2.0F;
        float ringCenterY = baseHeadY + HEAD_SIZE * HEAD_SCALE / 2.0F;
        renderCallRings(guiGraphics, ringCenterX, ringCenterY, time);

        if (moveTime >= 0.0F) {
            float nameProgress = Mth.clamp(moveTime / FINAL_NAME_ANIMATION_TICKS, 0.0F, 1.0F);
            nameProgress = easeOutCubic(nameProgress);
            renderFinalName(guiGraphics, headX, baseHeadY, nameProgress);
        }

        if (moveTime >= 0.0F) {
            renderUnderlineAndCalling(guiGraphics, moveTime, headX, baseHeadY);
        }

        renderHead(guiGraphics, frame, head, headX, baseHeadY, headFadeProgress);
    }

    private void renderHead(GuiGraphics guiGraphics, ResourceLocation frame, ResourceLocation head, int headX, int headY, float alpha) {
        if (alpha <= 0.0F) return;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(HEAD_SCALE, HEAD_SCALE, 1.0F);
        float scaledHeadX = headX / HEAD_SCALE;
        float scaledHeadY = headY / HEAD_SCALE;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        guiGraphics.blit(frame, (int) scaledHeadX, (int) scaledHeadY, 0, 0, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);

        if (sender.isCustomHead() || head == null) {
            if (head == null) {
                head = MineChatTextures.UNKNOW;
            }
            guiGraphics.blit(head, (int) scaledHeadX + 3, (int) scaledHeadY + 3, 0, 0, HEAD_INNER_SIZE, HEAD_INNER_SIZE, HEAD_INNER_SIZE, HEAD_INNER_SIZE);
        } else {
            guiGraphics.blit(head, (int) scaledHeadX + 3, (int) scaledHeadY + 3, 16, 16, HEAD_INNER_SIZE, HEAD_INNER_SIZE, 128, 128);
            guiGraphics.blit(head, (int) scaledHeadX + 3, (int) scaledHeadY + 3, 80, 16, HEAD_INNER_SIZE, HEAD_INNER_SIZE, 128, 128);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }

    private void renderFinalName(GuiGraphics guiGraphics, int headX, int headY, float progress) {
        int actualHeadWidth = (int) (HEAD_SIZE * HEAD_SCALE);
        int finalX = headX + actualHeadWidth + FINAL_NAME_OFFSET_X;
        int finalY = headY + actualHeadWidth / 2 - (int) (font.lineHeight * NAME_SCALE / 2.0F);
        int startX = finalX + FINAL_NAME_START_OFFSET_X + 10;
        int x = (int) Mth.lerp(progress, startX, finalX);
        int alpha = Mth.clamp((int) (progress * 255.0F), 30, 255);
        int color = (alpha << 24) | WHITE;
        drawScaledText(guiGraphics, Component.translatable(sender.getName() == null ? "Unknow" : sender.getName()), x, finalY, color, NAME_SCALE);
    }

    private void renderCallRings(GuiGraphics guiGraphics, float centerX, float centerY, float time) {
        float interval = RING_DURATION_TICKS / 2.0F;
        for (int i = 0; i < 3; i++) {
            float ringTime = time - RING_START_TICKS - i * interval;
            if (ringTime < 0.0F) continue;
            ringTime %= RING_DURATION_TICKS;
            float progress = ringTime / RING_DURATION_TICKS;
            progress = Mth.clamp(progress, 0.0F, 1.0F);
            float easedProgress = easeOutQuad(progress);
            float scale = Mth.lerp(easedProgress, RING_START_SCALE, RING_END_SCALE);
            float alphaProgress = easeOutQuad(progress);
            int alpha = (int) Mth.lerp(alphaProgress, RING_START_ALPHA, RING_END_ALPHA);
            alpha = Mth.clamp(alpha, 0, 255);
            if (alpha <= 0) continue;
            float baseSize = HEAD_SIZE * HEAD_SCALE;
            float width = baseSize * scale;
            float height = baseSize * scale;
            int left = (int) (centerX - width / 2.0F);
            int top = (int) (centerY - height / 2.0F);
            int right = (int) (centerX + width / 2.0F);
            int bottom = (int) (centerY + height / 2.0F);
            int color = (alpha << 24) | WHITE;
            guiGraphics.fill(left, top, right, top + 1, color);
            guiGraphics.fill(left, bottom - 1, right, bottom, color);
            guiGraphics.fill(left, top, left + 1, bottom, color);
            guiGraphics.fill(right - 1, top, right, bottom, color);
        }
    }

    private void renderUnderlineAndCalling(GuiGraphics guiGraphics, float underlineTime, int headX, int headY) {
        float underlineProgress = Mth.clamp(underlineTime / UNDERLINE_ANIMATION_TICKS, 0.0F, 1.0F);
        underlineProgress = easeOutCubic(underlineProgress);
        int underlineWidth = (int) (this.width * UNDERLINE_WIDTH_RATIO);
        int currentWidth = (int) (underlineWidth * underlineProgress);
        int centerX = this.width / 2;
        int underlineStartX = centerX - currentWidth / 2;
        int underlineEndX = centerX + currentWidth / 2;
        int actualHeadHeight = (int) (HEAD_SIZE * HEAD_SCALE);
        int underlineY = headY + actualHeadHeight + UNDERLINE_OFFSET_Y;
        int underlineAlpha = (int) (underlineProgress * 255.0F);
        underlineAlpha = Mth.clamp(underlineAlpha, 30, 255);
        int underlineColor = (underlineAlpha << 24) | WHITE;
        if (currentWidth > 0) {
            guiGraphics.fill(underlineStartX, underlineY, underlineEndX, underlineY + UNDERLINE_HEIGHT, underlineColor);
        }
        float callingProgress = Mth.clamp(underlineTime / CALLING_FADE_TICKS, 0.0F, 1.0F);
        callingProgress = easeOutCubic(callingProgress);
        renderCallingText(guiGraphics, underlineY, callingProgress);
    }

    private void renderCallingText(GuiGraphics guiGraphics, int underlineY, float progress) {
        Component text = Component.translatable("mine_chat.picture.npc_call.calling", Component.translatable(sender.getName() == null ? "Unknow" : sender.getName())).withStyle(ChatFormatting.YELLOW);
        int textWidth = (int) (this.font.width(text) * CALLING_SCALE);
        int x = this.width / 2 - textWidth / 2;
        int y = underlineY + UNDERLINE_HEIGHT + CALLING_OFFSET_Y;
        int alpha = Mth.clamp((int) (progress * 255.0F), 30, 255);
        int color = (alpha << 24) | WHITE;
        drawScaledText(guiGraphics, text, x, y, color, CALLING_SCALE);
    }

    private void drawScaledText(GuiGraphics guiGraphics, String text, int x, int y, int color, float scale) {
        if ((color >>> 24) <= 0) return;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0F);
        float scaledX = x / scale;
        float scaledY = y / scale;
        guiGraphics.drawString(this.font, text, scaledX, scaledY, color, true);
        guiGraphics.pose().popPose();
    }

    private void drawScaledText(GuiGraphics guiGraphics, Component text, int x, int y, int color, float scale) {
        if ((color >>> 24) <= 0) return;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0F);
        float scaledX = x / scale;
        float scaledY = y / scale;
        guiGraphics.drawString(this.font, text, Math.round(scaledX), Math.round(scaledY), color, true);
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}