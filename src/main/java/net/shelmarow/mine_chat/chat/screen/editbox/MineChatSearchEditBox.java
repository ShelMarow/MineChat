package net.shelmarow.mine_chat.chat.screen.editbox;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
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
public class MineChatSearchEditBox extends EditBox {

    private static final ResourceLocation PLAYER_SEARCH = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/player_search.png");
    private static final ResourceLocation PLAYER_SEARCH_INPUT = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/player_search_input.png");
    private final Font font;

    public MineChatSearchEditBox(Font pFont, int pX, int pY) {
        super(pFont, pX, pY, 74 - 15, 18, Component.translatable("text.mine_chat.player_search").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        setBordered(false);
        this.font = pFont;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.blit(PLAYER_SEARCH_INPUT, getX(), getY(), 0, 0, getWidth() + 15, getHeight(), getWidth() + 15, getHeight());
        if (!isFocused() && getValue().isEmpty()) {
            renderScrollingString(pGuiGraphics, font, getMessage(), getX() + 5, getY(), getX() + width + 10, getY() + height, 0xFFFFFF);
        }

        PoseStack poseStack = pGuiGraphics.pose();

        poseStack.pushPose();
        poseStack.translate(6, 6, 0);

        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        poseStack.popPose();
    }

    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return this.visible &&
                pMouseX >= (this.getX()) && pMouseX < (this.getX() + this.width + 15) &&
                pMouseY >= (this.getY()) && pMouseY < (this.getY() + this.height);
    }

    @Override
    protected boolean clicked(double pMouseX, double pMouseY) {
        return this.active && this.visible &&
                pMouseX >= (this.getX()) &&
                pMouseY >= (this.getY()) &&
                pMouseX < (this.getX() + this.width + 15) &&
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
    }
}
