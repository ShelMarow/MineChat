package net.shelmarow.mine_chat.chat.screen.editbox;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.sound.MineChatSounds;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MineChatCommonEditBox extends EditBox {

    private static final ResourceLocation EDIT_BOX4 = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/edit_box4.png");

    // @ 补全相关
    private final List<String> mentionSuggestions = new ArrayList<>();
    private int mentionAtPosition = -1;
    private boolean isMentionActive = false;
    private String mentionSuggestionText = "";
    private Font font;

    public MineChatCommonEditBox(Font pFont, int pX, int pY) {
        super(pFont, pX, pY, 358 - 19, 20, Component.empty());
        this.font = pFont;
        setBordered(false);
        this.isMentionActive = true;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.blit(EDIT_BOX4, getX(), getY(), 0, 0, getWidth() + 16, getHeight(), getWidth() + 16, getHeight());

        PoseStack poseStack = pGuiGraphics.pose();

        poseStack.pushPose();
        poseStack.translate(6, 6, 0);

        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        renderSuggestionText(pGuiGraphics);

        poseStack.popPose();
    }

    // 渲染灰色提示文本
    protected void renderSuggestionText(GuiGraphics guiGraphics) {
        if (!isMentionActive || mentionSuggestionText.isEmpty() || !isFocused()) {
            return;
        }

        String text = getValue();
        int cursorPos = getCursorPosition();

        // 计算光标位置
        String beforeCursor = text.substring(0, cursorPos);
        int textX = font.width(beforeCursor);

        // 查找 @ 位置
        int atIndex = text.lastIndexOf('@', cursorPos);
        if (atIndex == -1) {
            return;
        }

        String afterAt = text.substring(atIndex + 1, cursorPos);
        String suggestion = mentionSuggestionText;

        if (suggestion.toLowerCase().startsWith(afterAt.toLowerCase())) {
            String completion = suggestion.substring(afterAt.length());
            if (!completion.isEmpty()) {
                RenderSystem.enableBlend();
                guiGraphics.drawString(font, completion, getX() + textX, getY(), 0x88AAAAAA);
                RenderSystem.disableBlend();
            }
        }
    }

    // 更新 @ 建议
    public void updateMentionSuggestions() {
        String text = getValue();
        int cursorPos = getCursorPosition();

        // 查找 @ 符号位置
        int atIndex = text.lastIndexOf('@', cursorPos);

        if (atIndex == -1) {
            mentionSuggestionText = "";
            mentionSuggestions.clear();
            return;
        }

        // 检查 @ 到光标之间是否有空格
        String afterAt = text.substring(atIndex + 1, cursorPos);
        if (afterAt.contains(" ")) {
            mentionSuggestionText = "";
            mentionSuggestions.clear();
            return;
        }

        // 更新 @ 状态
        mentionAtPosition = atIndex;
        isMentionActive = true;

        // 获取在线玩家列表
        List<String> players = getOnlinePlayerNames();
        mentionSuggestions.clear();
        mentionSuggestionText = "";

        for (String name : players) {
            if (name.toLowerCase().startsWith(afterAt.toLowerCase())) {
                mentionSuggestions.add(name);
            }
        }
        Collections.sort(mentionSuggestions);

        // 如果有匹配，取第一个作为提示
        if (!mentionSuggestions.isEmpty()) {
            mentionSuggestionText = mentionSuggestions.getFirst();
        } else {
            mentionSuggestionText = "";
        }
    }


    public void clearMentionSuggestions() {
        mentionSuggestions.clear();
        mentionAtPosition = -1;
        mentionSuggestionText = "";
    }


    public void completeMention() {
        if (mentionSuggestionText.isEmpty() || mentionAtPosition == -1) return;

        String text = getValue();
        int cursorPos = getCursorPosition();

        String afterAt = text.substring(mentionAtPosition + 1, cursorPos);

        if (mentionSuggestionText.toLowerCase().startsWith(afterAt.toLowerCase())) {
            String beforeAt = text.substring(0, mentionAtPosition);
            String afterCursor = cursorPos < text.length() ? text.substring(cursorPos) : "";

            String newText = beforeAt + "@" + mentionSuggestionText + " " + afterCursor;
            setValue(newText);
            int newCursorPos = getCursorPosition() + mentionAtPosition + mentionSuggestionText.length();
            newCursorPos = Math.min(newCursorPos, newText.length());

            // 同时设置光标位置和高亮位置，清除选中状态
            setCursorPosition(newCursorPos);
            setHighlightPos(newCursorPos);

            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        clearMentionSuggestions();
    }

    // 获取在线玩家名称列表
    protected List<String> getOnlinePlayerNames() {
        List<String> names = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection != null && mc.player != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                String name = info.getProfile().getName();
                if (name != null) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB && !mentionSuggestionText.isEmpty()) {
            completeMention();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void setValue(@NotNull String pText) {
        super.setValue(pText);
        updateMentionSuggestions();
    }

    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return this.visible &&
                pMouseX >= (this.getX()) && pMouseX < (this.getX() + this.width + 16) &&
                pMouseY >= (this.getY()) && pMouseY < (this.getY() + this.height);
    }

    @Override
    protected boolean clicked(double pMouseX, double pMouseY) {
        return this.active && this.visible &&
                pMouseX >= (this.getX()) &&
                pMouseY >= (this.getY()) &&
                pMouseX < (this.getX() + this.width + 16) &&
                pMouseY < (this.getY() + this.height);
    }

    @Override
    public void playDownSound(SoundManager pHandler) {
        pHandler.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void insertText(@NotNull String pTextToWrite) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MineChatSounds.TYPING, 1.0F));
        super.insertText(pTextToWrite);
        updateMentionSuggestions();
    }

    @Override
    public void setFocused(boolean pFocused) {
        super.setFocused(pFocused);
        if (!pFocused) {
            clearMentionSuggestions();
        }
    }

    @Override
    public void moveCursorTo(int pPos, boolean pSelection) {
        super.moveCursorTo(pPos, pSelection);
        updateMentionSuggestions();
    }

    @Override
    public void setCursorPosition(int pPos) {
        super.setCursorPosition(pPos);
        updateMentionSuggestions();
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        boolean result = super.charTyped(pCodePoint, pModifiers);
        updateMentionSuggestions();
        return result;
    }
}