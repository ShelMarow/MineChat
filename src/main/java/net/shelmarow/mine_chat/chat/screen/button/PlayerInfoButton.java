package net.shelmarow.mine_chat.chat.screen.button;


import net.minecraft.ChatFormatting;
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
import net.shelmarow.mine_chat.chat.playercache.PlayerCache;
import net.shelmarow.mine_chat.chat.playercache.PlayerCacheManager;
import net.shelmarow.mine_chat.chat.texture.MineChatTextures;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class PlayerInfoButton extends AbstractButton {

    private static final ResourceLocation DM_PLAYER_INFO = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/dm_player_info.png");
    private static final ResourceLocation DM_PLAYER_INFO_HOVERED = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "textures/mchat/dm_player_info_hovered.png");

    private final PlayerInfoButton.OnPress onPress;
    private final Font font;

    private final PlayerCache cache;

    public PlayerInfoButton(Font font, int pX, int pY, PlayerCache cache, PlayerInfoButton.OnPress onPress) {
        super(pX, pY, 74, 20, Component.empty());
        this.onPress = onPress;
        this.font = font;
        this.cache = cache;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        renderScrollingString(
                guiGraphics, font,
                Component.literal(cache.getProfile().getName()).withStyle(cache.isOnline() ? ChatFormatting.WHITE : ChatFormatting.GRAY),
                getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFFFFFF
        );

        if(isHovered()) {
            guiGraphics.blit(DM_PLAYER_INFO_HOVERED, this.getX(), this.getY(), 0, 0, 74, 20, 74, 20);
        }
        else {
            guiGraphics.blit(DM_PLAYER_INFO, this.getX(), this.getY(), 0, 0, 74, 20, 74, 20);
        }

        if(MineChatManager.isDMPlayerMessageUnread(cache.getProfile().getId())) {
            guiGraphics.blit(MineChatTextures.RED_POINT, this.getX() + getWidth() - 6, this.getY(), 0, 0, 6, 6, 6, 6);
        }
    }

    public void updateOnlineStatues() {
        boolean online = PlayerCacheManager.checkPlayerOnline(cache.getProfile().getId());
        cache.updateOnlineStatus(online);
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
        void onPress(PlayerInfoButton var1);
    }
}
