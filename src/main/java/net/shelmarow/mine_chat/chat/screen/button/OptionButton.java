package net.shelmarow.mine_chat.chat.screen.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shelmarow.mine_chat.MineChat;
import org.jetbrains.annotations.NotNull;

public class OptionButton extends AbstractButton {

    private static final ResourceLocation DM_PLAYER_INFO = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/button.png");
    private static final ResourceLocation DM_PLAYER_INFO_HOVERED = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/button_hovered.png");

    private final OnPress onPress;

    public OptionButton(int x, int y, Component message, OnPress onPress) {
        super(x, y, 294, 20, message);
        this.onPress = onPress;
    }


    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if(isHovered()) {
            guiGraphics.blit(DM_PLAYER_INFO_HOVERED, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
        }
        else {
            guiGraphics.blit(DM_PLAYER_INFO, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
        }
        this.renderString(guiGraphics, Minecraft.getInstance().font, getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24);
    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        super.defaultButtonNarrationText(narrationElementOutput);
    }

    public interface OnPress{
        void onPress(OptionButton button);
    }
}
