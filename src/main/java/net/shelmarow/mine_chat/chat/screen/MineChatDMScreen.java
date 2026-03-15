package net.shelmarow.mine_chat.chat.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.message.AnimationMessage;
import net.shelmarow.mine_chat.chat.playercache.PlayerCache;
import net.shelmarow.mine_chat.chat.playercache.PlayerCacheManager;
import net.shelmarow.mine_chat.chat.screen.button.PlayerInfoButton;
import net.shelmarow.mine_chat.chat.screen.editbox.MineChatDMEditBox;
import net.shelmarow.mine_chat.chat.screen.editbox.MineChatSearchEditBox;
import net.shelmarow.mine_chat.chat.texture.MineChatTextures;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class MineChatDMScreen extends MineChatScreen{

    private UUID targetUUID = Util.NIL_UUID;
    private MineChatSearchEditBox searchBox;
    private String searchText = "";
    private final List<InfoButton> renderableList = new ArrayList<>();
    private int infoScrollDelta = 0;
    private int totalInfoHeight = 0;
    private ScissorBound buttonBound;


    public MineChatDMScreen() {
        super();
        currentPage = CurrentPage.DM;
        background = MineChatTextures.DM_CHANNEL;
    }

    public MineChatDMScreen(String searchText) {
        this();
        this.searchText = searchText;
        PlayerCache cache = PlayerCacheManager.getPlayerCache(searchText, false);
        if(cache != null) {
            targetUUID = cache.getProfile().getId();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(updateTick == maxUpdateTick){
            for (InfoButton infoButton : renderableList) {
                infoButton.infoButton.updateOnlineStatues();
            }
        }
    }

    @Override
    protected void reflashScreen() {
        searchText = "";
        infoScrollDelta = 0;
        targetUUID = Util.NIL_UUID;

        super.reflashScreen();
    }

    public void reflashPlayerInfo(){
        this.removeWidget(mainEditBox);

        for(InfoButton infoButton : renderableList){
            this.removeWidget(infoButton.infoButton);
        }
        addPlayerInfoButtons();
        addMainEditBox();
    }

    @Override
    protected void initRenderOffset(){
        nameLeftOffsetX = 80;
        nameRightOffsetX = 0;
        messageLeftOffsetX = 80;
        frameLeftOffsetX = 80;
    }

    @Override
    protected void initScissorPos() {
        int scissorStartX = centerX - bgWidth / 2 + 92;
        int scissorStartY = centerY - bgHeight / 2 + 47;
        int scissorStartEndX = scissorStartX + 292;
        int scissorStartEndY = scissorStartY + 138;
        messageBound = new ScissorBound(scissorStartX, scissorStartY, scissorStartEndX, scissorStartEndY);

        int bStartX = startX + 13;
        int bStartY = startY + 47;
        int bEndX = startX + 87;
        int bEndY = startY + 202;
        buttonBound = new ScissorBound(bStartX, bStartY, bEndX, bEndY);


    }

    @Override
    protected void addScreenWidgets() {
        addPlayerInfoButtons();

        //添加搜索栏
        this.searchBox = new MineChatSearchEditBox(font, startX + 13, startY + 29);
        addRenderableWidget(this.searchBox);

        addMainEditBox();

    }

    private void addMainEditBox() {
        //添加输入栏
        if(!targetUUID.equals(Util.NIL_UUID)) {
            mainEditBox = new MineChatDMEditBox(font, startX + 91, startY + 185, 374, 20);
            this.mainEditBox.setMaxLength(256);
            addRenderableWidget(this.mainEditBox);
            this.setInitialFocus(this.mainEditBox);
        }
    }

    private void addPlayerInfoButtons() {
        renderableList.clear();

        int i = 0;
        for (PlayerCache cache : MineChatManager.getDMPlayers(this.searchText)) {

            PlayerInfoButton button = new PlayerInfoButton(font, startX + 13, startY + 29 + 18 + i * 20, cache, b -> {
                UUID uuid = cache.getProfile().getId();
                targetUUID = targetUUID.equals(uuid) ? Util.NIL_UUID : uuid;
                reflashPlayerInfo();
            });
            button.updateOnlineStatues();
            addWidget(button);
            renderableList.add(new InfoButton(button));

            i++;
        }
        totalInfoHeight = i * 20;
    }

    @Override
    protected void renderBeforeScissor(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        //渲染标题名称
        Component displayName = Component.translatable("text.mine_chat.select_player");
        int titleCenterX = 91 + 297 / 2 - font.width(displayName) / 2;
        int titleCenterY = 24 + 184 / 2 - 6;
        if(!targetUUID.equals(Util.NIL_UUID)) {
            PlayerCache cache = PlayerCacheManager.getPlayerCache(targetUUID);
            if(cache != null) {

                displayName = Component.literal(cache.getProfile().getName())
                        .append(Component.translatable(cache.isOnline() ? "text.mine_chat.online" : "text.mine_chat.offline"))
                        .withStyle(cache.isOnline() ? ChatFormatting.WHITE : ChatFormatting.GRAY);

                titleCenterX = 91 + 294 / 2 - font.width(displayName) / 2;
                titleCenterY = 27 + 10 - 4;

                //头像
                poseStack.pushPose();
                poseStack.translate(titleCenterX - 16, 29 + 2, 0);
                poseStack.scale(1.5F, 1.5F, 1);

                ResourceLocation head = cache.getSkinLocation();
                guiGraphics.blit(head, 0, 0, 8, 8, 8, 8, 64, 64);
                guiGraphics.blit(head, 0, 0, 40, 8, 8, 8, 64, 64);

                poseStack.popPose();
            }
        }
        guiGraphics.drawString(font, displayName, titleCenterX, titleCenterY, 0xFFFFFF);
    }

    @Override
    protected void renderAfterRenderable(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {

        guiGraphics.enableScissor(startX + 13, startY + 48, startX + 87, startY + 201);

        for(InfoButton renderable : this.renderableList) {

            renderable.infoButton.setY(renderable.baseY + infoScrollDelta);

            if(buttonBound.inScissorBound(renderable.infoButton.getX(), renderable.infoButton.getY(), renderable.infoButton.getWidth(), renderable.infoButton.getHeight())) {
                renderable.infoButton.render(guiGraphics, mouseX, mouseY, partialTick);
                renderable.infoButton.active = true;
            }
            else {
                renderable.infoButton.active = false;
            }
        }

        guiGraphics.disableScissor();

    }

    @Override
    public @NotNull List<AnimationMessage> getChatMessages() {
        if(targetUUID.equals(Util.NIL_UUID)) {
            return new ArrayList<>();
        }
        List<AnimationMessage> chatMessages = MineChatManager.getDMMessages(targetUUID);
        Collections.reverse(chatMessages);
        MineChatManager.checkDM(targetUUID);
        return chatMessages;
    }


    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.isActive() && (pKeyCode == 257 || pKeyCode == 335)) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                //targetUUID = Util.NIL_UUID;
                searchText = this.searchBox.getValue();
                reflashPlayerInfo();
            }
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    protected void onEditBoxEnterPressed(LocalPlayer player) {
        super.onEditBoxEnterPressed(player);
        if(currentPage == CurrentPage.DM) {
            if(!targetUUID.equals(Util.NIL_UUID)) {
                String message = this.mainEditBox.getValue();
                PlayerCache playerCache = PlayerCacheManager.getPlayerCache(targetUUID);
                if(playerCache != null && playerCache.isOnline() && !message.isEmpty()) {
                    player.connection.sendCommand("tell " + playerCache.getProfile().getName() + " " + message);
                    resetScroll();
                }
            }
        }
        this.mainEditBox.setValue("");
    }



    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        pDelta = pDelta == 0 ? 0 : (pDelta < 0 ? -1 : 1);
        if (!hasShiftDown()) {
            pDelta *= 7;
        }

        if(buttonBound.inScissorBound((int) pMouseX, (int) pMouseY, 0, 0)){
            if(totalInfoHeight > buttonBound.totalYHeight()) {
                this.infoScrollDelta = (int) Mth.clamp(this.infoScrollDelta + pDelta, -(this.totalInfoHeight - buttonBound.totalYHeight()), 0);
            }
            return true;
        }
        else {
            return super.mouseScrolled(pMouseX, pMouseY, pDelta);
        }
    }

    public UUID getSelectedTarget() {
        return targetUUID;
    }

    public String getSearchText() {
        return searchText;
    }

    public static class InfoButton{
        PlayerInfoButton infoButton;
        int baseY;
        int baseX;

        public InfoButton(PlayerInfoButton infoButton) {
            this.infoButton = infoButton;
            baseX = infoButton.getX();
            baseY = infoButton.getY();
        }
    }
}
