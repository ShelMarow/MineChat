package net.shelmarow.mine_chat.chat.screen.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import net.shelmarow.mine_chat.chat.picture.data.ChatPicture;
import org.jetbrains.annotations.NotNull;

public class SendPictureButton extends AbstractButton {
    public static final ResourceLocation SEND_PICTURE = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/send_picture.png");

    private final OnPress onPress;
    private final OnPress onRightClick;
    private final ChatPicture picture;

    public SendPictureButton(int x, int y, Component message, OnPress onPress, OnPress onRightClick) {
        super(x, y, 20, 20, message);
        this.onPress = onPress;
        this.onRightClick = onRightClick;
        picture = ClientPictureManager.getInstance().getPicture("mine_chat:cat|chat");
        setTooltip(Tooltip.create(Component.translatable("text.mine_chat.send_picture_tooltip")));
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if(isHovered()) {
            guiGraphics.renderOutline(this.getX(), this.getY(), getWidth(), getHeight(), 0xFFFFFFFF);
        }
        else {
            guiGraphics.blit(SEND_PICTURE, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        if(picture != null) {
            ResourceLocation gifTexture = picture.getTexture();
            guiGraphics.blit(gifTexture, this.getX() + 2, this.getY() + 2, 0, 0, this.width - 4, this.height - 4, this.width - 4, this.height - 4);
        }

        Font font = Minecraft.getInstance().font;
        this.renderString(guiGraphics, font, getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24);
    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        super.defaultButtonNarrationText(narrationElementOutput);
    }

    public interface OnPress {
        void onPress(SendPictureButton button);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible) {
            if (this.isValidClickButton(button)) {
                boolean flag = this.clicked(mouseX, mouseY);
                if (flag) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    this.onPress();  // 左键
                    return true;
                }
            }
            // 检测右键（button == 1）
            if (button == 1 && this.clicked(mouseX, mouseY)) {
                if (onRightClick != null) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    onRightClick.onPress(this);
                    return true;
                }
            }
        }
        return false;
    }
}
