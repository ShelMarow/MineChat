package net.shelmarow.mine_chat.chat.screen.button;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.texture.MineChatTextures;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class NPCButton extends AbstractButton {

    private static final ResourceLocation DM_PLAYER_INFO = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/dm_player_info.png");
    private static final ResourceLocation DM_PLAYER_INFO_HOVERED = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/dm_player_info_hovered.png");

    private final OnPress onPress;
    private final Font font;
    private final ChatSender cache;

    public NPCButton(Font font, int pX, int pY, ChatSender cache, OnPress onPress) {
        super(pX, pY, 74, 20, Component.empty());
        this.onPress = onPress;
        this.font = font;
        this.cache = cache;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if(isHovered()) {
            guiGraphics.blit(DM_PLAYER_INFO_HOVERED, this.getX(), this.getY(), 0, 0, 74, 20, 74, 20);
        }
        else {
            guiGraphics.blit(DM_PLAYER_INFO, this.getX(), this.getY(), 0, 0, 74, 20, 74, 20);
        }

        if(cache.getHead() != null) {
            if(cache.isCustomHead()){
                guiGraphics.blit(cache.getHead(), this.getX() + 4, this.getY() + 6, 0, 0, 8, 8, 8, 8);
            }
            else {
                guiGraphics.blit(cache.getHead(), getX() + 4, getY() + 6, 8, 8, 8, 8, 64, 64);
                guiGraphics.blit(cache.getHead(), getX() + 4, getY() + 6, 40, 8, 8, 8, 64, 64);
            }
        }

        renderScrollingString(
                guiGraphics, font,
                Component.translatable(cache.getName() == null ? "Unknown" : cache.getName()),
                getX() + 14, getY(), getX() + getWidth() - 3, getY() + getHeight(), 0xFFFFFF
        );


        if(MineChatManager.isNPCMessageUnread(cache.getUuid())) {
            guiGraphics.blit(MineChatTextures.RED_POINT, this.getX() + getWidth() - 6, this.getY(), 0, 0, 6, 6, 6, 6);
        }
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
        void onPress(NPCButton var1);
    }
}
