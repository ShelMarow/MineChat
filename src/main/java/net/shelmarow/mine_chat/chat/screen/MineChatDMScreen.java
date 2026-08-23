package net.shelmarow.mine_chat.chat.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.message.AnimationMessage;
import net.shelmarow.mine_chat.chat.npc.ClientDialogProcessHandler;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import net.shelmarow.mine_chat.chat.playercache.PlayerCache;
import net.shelmarow.mine_chat.chat.playercache.PlayerCacheManager;
import net.shelmarow.mine_chat.chat.screen.button.NPCButton;
import net.shelmarow.mine_chat.chat.screen.button.OptionButton;
import net.shelmarow.mine_chat.chat.screen.button.PlayerInfoButton;
import net.shelmarow.mine_chat.chat.screen.editbox.MineChatDMEditBox;
import net.shelmarow.mine_chat.chat.screen.editbox.MineChatSearchEditBox;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.NPCSenderManager;
import net.shelmarow.mine_chat.chat.texture.MineChatTextures;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class MineChatDMScreen extends MineChatScreen {

    private final List<InfoButton> renderableList = new ArrayList<>();
    private final List<AbstractWidget> optionButtons = new ArrayList<>();
    private UUID targetUUID = Util.NIL_UUID;
    private boolean isPlayer = true;
    private MineChatSearchEditBox searchBox;
    private String searchText = "";
    private int infoScrollDelta = 0;
    private int totalInfoHeight = 0;
    private ScissorBound buttonBound;
    private boolean haveOptionButton = false;


    public MineChatDMScreen() {
        super();
        currentPage = CurrentPage.DM;
        background = MineChatTextures.DM_CHANNEL;
    }

    public MineChatDMScreen(String searchText) {
        this();
        this.searchText = searchText;
        PlayerCache cache = PlayerCacheManager.getPlayerCache(searchText, false);
        if (cache != null) {
            targetUUID = cache.getUuid();
            isPlayer = true;
        }
    }

    public MineChatDMScreen(UUID targetUUID) {
        this();
        if(NPCSenderManager.getInstance().getNpcData(targetUUID) != null){
            this.targetUUID = targetUUID;
            isPlayer = false;
        }
        startAnimation();
    }

    @Override
    public void tick() {
        super.tick();
        if (updateTick == maxUpdateTick) {
            for (InfoButton infoButton : renderableList) {
                if (infoButton.infoButton instanceof PlayerInfoButton playerInfoButton) {
                    playerInfoButton.updateOnlineStatues();
                }
            }
        }

        LocalPlayer player = getMinecraft().player;
        if (!isPlayer && !targetUUID.equals(Util.NIL_UUID) && player != null) {
            ChatSender npcData = NPCSenderManager.getInstance().getNpcData(targetUUID);
            if (npcData != null) {

                ClientDialogProcessHandler instance = ClientDialogProcessHandler.getInstance();
                ClientDialogProcessHandler.DialogActionProcesser action = instance.processDialogAction(npcData, player);

                if (action != null && !action.isFinished() && action.shouldDisplayOption() && !haveOptionButton) {
                    haveOptionButton = true;
                    reflashInfo();
                }
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

    public void reflashInfo() {
        this.showPicturePanel = false;
        this.scrollDelta = 0;
        if(mainEditBox != null) {
            this.removeWidget(mainEditBox);
        }
        this.removeWidget(pictureButton);

        for (InfoButton infoButton : renderableList) {
            this.removeWidget(infoButton.infoButton);
        }
        addSenderInfoButtons();

        if (isPlayer) {
            addMainEditBox();
            if (!targetUUID.equals(Util.NIL_UUID)) {
                addPictureButton();
            }
        }

        if (showPicturePanel) {
            createPictureButtons(picturePage, false);
        } else {
            clearPictureButtons();
        }

        addOptionButton();
    }

    @Override
    protected void initRenderOffset() {
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
        addSenderInfoButtons();

        //添加搜索栏
        this.searchBox = new MineChatSearchEditBox(font, startX + 13, startY + 29);
        addRenderableWidget(this.searchBox);
        searchBox.setResponder(text -> {
            searchText = text;
            reflashInfo();
        });

        //添加输入栏
        if (!targetUUID.equals(Util.NIL_UUID)) {
            if (isPlayer) {
                addMainEditBox();
                addPictureButton();
            }
        }

    }

    protected void addMainEditBox() {
        if (!targetUUID.equals(Util.NIL_UUID)) {
            mainEditBox = new MineChatDMEditBox(font, startX + 91, startY + 185, 374, 20);
            this.mainEditBox.setMaxLength(256);
            addRenderableWidget(this.mainEditBox);
            this.setInitialFocus(this.mainEditBox);
        }
    }


    private void addOptionButton() {
        for (AbstractWidget optionButton : optionButtons) {
            removeWidget(optionButton);
        }
        //添加NPC选项按钮
        if (!targetUUID.equals(Util.NIL_UUID) && !isPlayer && haveOptionButton) {
            ClientDialogProcessHandler instance = ClientDialogProcessHandler.getInstance();
            ClientDialogProcessHandler.DialogActionProcesser processer = instance.getCurrentAction(targetUUID);
            if (processer != null) {
                List<String> options = processer.getAction().getOptions();
                if (!options.isEmpty()) {
                    OptionButton button = new OptionButton(startX + 91, startY + 185, Component.translatable(options.get(0)), b -> {
                        if (getMinecraft().player != null) {
                            MineChatManager.sendToNPC(getMinecraft().player, targetUUID, Component.translatable(options.get(0)));
                        }
                        processer.setFinished(true);
                        haveOptionButton = false;
                        reflashInfo();
                    });
                    optionButtons.add(button);
                    addRenderableWidget(button);
                }
            }
        }
    }

    private void addSenderInfoButtons() {
        renderableList.clear();
        int offsetY = 0;
        for (MineChatManager.ChatTarget target : MineChatManager.getChatTargets(this.searchText)) {
            if (target.isPlayer()) {
                PlayerCache cache = target.getPlayerCache();
                PlayerInfoButton button = new PlayerInfoButton(font, startX + 13, startY + 29 + 18 + offsetY, cache.getUuid(), b -> {
                    UUID uuid = cache.getUuid();
                    targetUUID = targetUUID.equals(uuid) ? Util.NIL_UUID : uuid;
                    isPlayer = true;
                    reflashInfo();
                });
                button.updateOnlineStatues();
                addWidget(button);
                renderableList.add(new InfoButton(button));
            }
            else {
                ChatSender sender = target.getSender();
                NPCButton npcButton = new NPCButton(font, startX + 13, startY + 29 + 18 + offsetY, sender, b -> {
                    UUID uuid = sender.getUuid();
                    targetUUID = targetUUID.equals(uuid) ? Util.NIL_UUID : uuid;
                    isPlayer = false;
                    reflashInfo();
                });
                addWidget(npcButton);
                renderableList.add(new InfoButton(npcButton));
            }
            offsetY += 20;
        }
        totalInfoHeight = offsetY;
    }

    @Override
    protected void renderBeforeScissor(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {

        //渲染标题名称
        MutableComponent displayName = Component.translatable("text.mine_chat.select_player");
        int titleCenterX = 91 + 297 / 2 - font.width(displayName) / 2;
        int titleCenterY = 24 + 184 / 2 - 6;
        if (!targetUUID.equals(Util.NIL_UUID)) {
            if (isPlayer) {
                PlayerCache cache = PlayerCacheManager.getPlayerCache(targetUUID);
                if (cache != null) {

                    displayName = Component.literal(cache.getName()).append(Component.translatable(cache.isOnline() ? "text.mine_chat.online" : "text.mine_chat.offline")).withStyle(cache.isOnline() ? ChatFormatting.WHITE : ChatFormatting.GRAY);

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
            } else {
                ChatSender chatSender = NPCSenderManager.getInstance().getNpcData(targetUUID);
                if (chatSender != null) {
                    displayName = Component.translatable(chatSender.getName() == null ? "Unknown" : chatSender.getName());

                    titleCenterX = 91 + 294 / 2 - font.width(displayName) / 2;
                    titleCenterY = 27 + 10 - 4;

                    if (chatSender.getHead() != null) {
                        //头像
                        poseStack.pushPose();
                        poseStack.translate(titleCenterX - 16, 29 + 2, 0);
                        poseStack.scale(1.5F, 1.5F, 1);

                        ResourceLocation head = chatSender.getHead();

                        if (chatSender.isCustomHead()) {
                            guiGraphics.blit(head, 0, 0, 0, 0, 8, 8, 8, 8);
                        } else {
                            guiGraphics.blit(head, 0, 0, 8, 8, 8, 8, 64, 64);
                            guiGraphics.blit(head, 0, 0, 40, 8, 8, 8, 64, 64);
                        }

                        poseStack.popPose();
                    }
                }
            }
        }
        guiGraphics.drawString(font, displayName, titleCenterX, titleCenterY, 0xFFFFFF);
    }

    @Override
    protected void renderAfterRenderable(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {

        guiGraphics.enableScissor(startX + 13, startY + 48, startX + 87, startY + 201);

        for (InfoButton renderable : this.renderableList) {

            renderable.infoButton.setY(renderable.baseY + infoScrollDelta);

            if (buttonBound.inScissorBound(renderable.infoButton.getX(), renderable.infoButton.getY(), renderable.infoButton.getWidth(), renderable.infoButton.getHeight())) {
                renderable.infoButton.render(guiGraphics, mouseX, mouseY, partialTick);
                renderable.infoButton.active = true;
            } else {
                renderable.infoButton.active = false;
            }
        }

        guiGraphics.disableScissor();

    }

    @Override
    public @NotNull List<AnimationMessage> getChatMessages() {
        if (targetUUID.equals(Util.NIL_UUID)) {
            return new ArrayList<>();
        }
        List<AnimationMessage> chatMessages = new ArrayList<>();
        if (isPlayer) {
            chatMessages.addAll(MineChatManager.getDMMessages(targetUUID));
        } else {
            chatMessages.addAll(MineChatManager.getNPCMessages(targetUUID));
        }
        Collections.reverse(chatMessages);
        MineChatManager.checkDM(targetUUID, isPlayer);
        return chatMessages;
    }


    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.isActive() && (pKeyCode == 257 || pKeyCode == 335)) {
            searchText = this.searchBox.getValue();
            reflashInfo();
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    protected void onEditBoxEnterPressed(LocalPlayer player) {
        if(mainEditBox != null) {
            super.onEditBoxEnterPressed(player);
            if (!targetUUID.equals(Util.NIL_UUID)) {
                String message = this.mainEditBox.getValue();
                PlayerCache playerCache = PlayerCacheManager.getPlayerCache(targetUUID);
                if (playerCache != null && playerCache.isOnline() && !message.isEmpty()) {
                    String messageCommand = "tell " + playerCache.getName() + " " + message;
                    player.connection.sendCommand(messageCommand);
                    if(!mainEditBox.getValue().isEmpty() && !ClientPictureManager.getInstance().isPicture(mainEditBox.getValue())) {
                        getMinecraft().gui.getChat().addRecentChat(message);
                    }
                    resetScroll();
                }
            }
            this.mainEditBox.setValue("");
        }
    }


    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double delta) {
        if (buttonBound.inScissorBound((int) pMouseX, (int) pMouseY, 0, 0)) {
            if (!hasShiftDown()) {
                delta *= 10;
            }
            if (totalInfoHeight > buttonBound.totalYHeight()) {
                this.infoScrollDelta = (int) Mth.clamp(this.infoScrollDelta + delta, -(this.totalInfoHeight - buttonBound.totalYHeight()), 0);
            }
            return true;
        } else {
            return super.mouseScrolled(pMouseX, pMouseY, delta);
        }
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        reflashInfo();
    }

    public UUID getSelectedTarget() {
        return targetUUID;
    }

    public String getSearchText() {
        return searchText;
    }

    public static class InfoButton {
        AbstractWidget infoButton;
        int baseY;
        int baseX;

        public InfoButton(AbstractWidget infoButton) {
            this.infoButton = infoButton;
            baseX = infoButton.getX();
            baseY = infoButton.getY();
        }
    }
}
