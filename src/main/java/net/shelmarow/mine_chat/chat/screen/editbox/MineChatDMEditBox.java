package net.shelmarow.mine_chat.chat.screen.editbox;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.sound.MineChatSounds;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class MineChatDMEditBox extends EditBox {

    private static final ResourceLocation EDIT_BOX1 = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/edit_box2.png");

    public MineChatDMEditBox(Font pFont, int pX, int pY, int pWidth, int pHeight) {
        super(pFont, pX, pY, pWidth - 16, pHeight, Component.empty());
        setBordered(false);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.blit(EDIT_BOX1, getX(), getY(), 0, 0, getWidth() + 16, getHeight(), getWidth() + 16, getHeight());

        PoseStack poseStack = pGuiGraphics.pose();

        poseStack.pushPose();
        poseStack.translate(6, 6, 0);

        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        poseStack.popPose();
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
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MineChatSounds.TYPING.get(), 1.0F));
        super.insertText(pTextToWrite);
    }
}
