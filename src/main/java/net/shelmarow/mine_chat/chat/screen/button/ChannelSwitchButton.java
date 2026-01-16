package net.shelmarow.mine_chat.chat.screen.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.screen.MineChatScreen;
import net.shelmarow.mine_chat.chat.texture.MineChatTextures;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ChannelSwitchButton extends AbstractButton {
    //按钮材质
    private static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/channel.png");
    private static final ResourceLocation CHANNEL_HOVERED = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/channel_hovered.png");
    private static final ResourceLocation CHANNEL_SELECTED = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/channel_selected.png");
    private static final ResourceLocation CHANNEL_SELECTED_HOVERED = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/channel_selected_hovered.png");

    protected final ChannelSwitchButton.OnPress onPress;
    protected final boolean selected;
    protected final MineChatScreen.CurrentPage currentPage;
    protected boolean unchecked = false;
    protected Component toolTip;

    public ChannelSwitchButton(int pX, int pY, boolean selected, MineChatScreen.CurrentPage currentPage, Component pMessage, ChannelSwitchButton.OnPress pOnPress) {
        this(pX, pY, selected, currentPage, pMessage, Component.empty(), pOnPress);
    }

    public ChannelSwitchButton(int pX, int pY, boolean selected, MineChatScreen.CurrentPage currentPage, Component pMessage, Component toolTip, ChannelSwitchButton.OnPress pOnPress) {
        super(pX, pY, 52, 16, pMessage);
        this.selected = selected;
        this.onPress = pOnPress;
        this.currentPage = currentPage;
        this.toolTip = toolTip;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        renderString(guiGraphics, mc.font,0xFFFFFF);

        if(selected) {
            if(isHovered()) {
                if(!toolTip.equals(Component.empty())) {
                    guiGraphics.renderTooltip(Minecraft.getInstance().font, toolTip, mouseX, mouseY);
                }
                guiGraphics.blit(CHANNEL_SELECTED_HOVERED, this.getX(), this.getY(), 0, 0, 52, 19, 52, 19);
            }
            else {
                guiGraphics.blit(CHANNEL_SELECTED, this.getX(), this.getY(), 0, 0, 52, 19, 52, 19);
            }
        }
        else{
            if(isHovered()) {
                guiGraphics.blit(CHANNEL_HOVERED, this.getX(), this.getY(), 0, 0, 52, 16, 52, 16);
            }
            else{
                guiGraphics.blit(CHANNEL, this.getX(), this.getY(), 0, 0, 52, 16, 52, 16);
            }
        }


        if(unchecked) {
            guiGraphics.blit(MineChatTextures.RED_POINT, this.getX() + getWidth() - 6, this.getY(), 0, 0, 6, 6, 6, 6);
        }
    }

    public void updateUnchecked() {
        this.unchecked =
                (currentPage == MineChatScreen.CurrentPage.DM && MineChatManager.isDMChatUnchecked() ||
                        currentPage == MineChatScreen.CurrentPage.TEAM && MineChatManager.isTeamChatUnchecked());
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput pNarrationElementOutput) {
        this.defaultButtonNarrationText(pNarrationElementOutput);
    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    @OnlyIn(Dist.CLIENT)
    public interface OnPress {
        void onPress(ChannelSwitchButton var1);
    }
}
