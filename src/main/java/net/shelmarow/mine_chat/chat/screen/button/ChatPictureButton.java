package net.shelmarow.mine_chat.chat.screen.button;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.shelmarow.mine_chat.chat.picture.data.ChatPicture;
import org.jetbrains.annotations.NotNull;

public class ChatPictureButton extends AbstractButton {

    private final ChatPicture picture;
    private final OnPress onPress;

    public ChatPictureButton(int x, int y, int width, int height, ChatPicture picture, OnPress onPress) {
        super(x, y, width, height, Component.empty());
        this.picture = picture.copy();
        this.onPress = onPress;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        Vec2 pSize = picture.getDisplaySize(width - 2, height - 2);
        int centerX = (int) (getX() + (getWidth() - pSize.x) / 2F);
        int centerY = (int) (getY() + (getHeight() - pSize.y) / 2F);
        if(isHovered()) {
            guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFFFFFFF);
        }

        if(picture.isGif() && isHovered()){
            picture.updateGif();
            guiGraphics.blit(picture.getGifTexture(), centerX, centerY, 0, 0, (int) pSize.x, (int) pSize.y, (int) pSize.x, (int) pSize.y);
        }
        else {
            guiGraphics.blit(picture.getTexture(), centerX, centerY, 0, 0, (int) pSize.x, (int) pSize.y, (int) pSize.x, (int) pSize.y);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }


    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    public interface OnPress {
        void onPress(ChatPictureButton button);
    }
}
