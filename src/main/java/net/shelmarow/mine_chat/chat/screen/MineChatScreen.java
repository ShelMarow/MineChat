package net.shelmarow.mine_chat.chat.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.message.AnimationMessage;
import net.shelmarow.mine_chat.chat.message.chat_enum.MessageType;
import net.shelmarow.mine_chat.chat.playercache.PlayerCache;
import net.shelmarow.mine_chat.chat.playercache.PlayerCacheManager;
import net.shelmarow.mine_chat.chat.screen.button.ChannelSwitchButton;
import net.shelmarow.mine_chat.chat.screen.editbox.MineChatCommonEditBox;
import net.shelmarow.mine_chat.chat.sound.MineChatSounds;
import net.shelmarow.mine_chat.chat.texture.MineChatTextures;
import net.shelmarow.mine_chat.config.MineChatConfig;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public abstract class MineChatScreen extends Screen {

    protected ResourceLocation background = MineChatTextures.COMMON_CHANNEL;

    protected final List<RenderedChatLine> renderedLines = new ArrayList<>();
    protected final List<ChannelSwitchButton> channelSwitchButtons = new ArrayList<>();

    protected CurrentPage currentPage = CurrentPage.GLOBE;

    protected EditBox mainEditBox;

    //每隔一定时间更新，减少负载
    protected int maxUpdateTick = 2;
    protected int updateTick = 0;

    //滚动设置
    protected int scrollDelta = 0;
    protected float totalLineHeight = 0;

    //位置设置
    protected int baseOffsetY = 30;
    protected int maxLineWidth = 150;
    protected int nameLeftOffsetX = 0;
    protected int nameRightOffsetX = 4;
    protected int messageLeftOffsetX = 0;
    protected int messageRightOffsetX = 0;
    protected int frameLeftOffsetX = 0;
    protected int frameRightOffsetX = 0;
    protected int bgWidth = 396;
    protected int bgHeight = 216;
    protected int centerX;
    protected int centerY;
    protected int startX;
    protected int startY;
    protected ScissorBound messageBound;

    //界面打开的时间
    protected long guiOpenTime = Long.MAX_VALUE;

    //GUI动画参数
    protected static boolean animationStarted = false;
    protected static float animationTimer = 0;
    protected static List<AnimationParam> animationParams = new ArrayList<>();

    //显示的消息
    protected float screenPartialTick = 0;
    protected List<AnimationMessage> displayedMessages = new ArrayList<>();

    public MineChatScreen() {
        super(Component.empty());
        initAnimation();
        Minecraft mc = Minecraft.getInstance();
        if(mc.level != null) {
            this.guiOpenTime = mc.level.getGameTime();
        }
        if(!animationStarted) {
            startAnimation();
        }
    }

    @Override
    public void init() {
        this.clearWidgets();
        Minecraft mc = Minecraft.getInstance();

        //初始化绘制开始位置
        initStartPos();
        //初始化消息偏移量
        initRenderOffset();
        //初始化裁剪区域
        initScissorPos();
        //添加标签按钮
        addTabButtons(mc);
        //添加输入栏
        addScreenWidgets();
    }

    protected void initRenderOffset(){
        nameLeftOffsetX = 0;
        nameRightOffsetX = 4;
        messageLeftOffsetX = 0;
        messageRightOffsetX = 0;
        frameLeftOffsetX = 0;
        frameRightOffsetX = 0;
    }

    protected void initScissorPos() {
        int scissorStartX = centerX - bgWidth / 2 + 12;
        int scissorStartY = centerY - bgHeight / 2 + 28;
        int scissorStartEndX = scissorStartX + 372;
        int scissorStartEndY = scissorStartY + 157;
        messageBound = new ScissorBound(scissorStartX, scissorStartY, scissorStartEndX, scissorStartEndY);
    }

    protected void initStartPos() {
        centerX = width / 2;
        centerY = height / 2;
        startX = centerX - bgWidth / 2;
        startY = centerY - bgHeight / 2;
    }

    protected void addScreenWidgets() {
        //添加输入栏
        mainEditBox = new MineChatCommonEditBox(font, startX + 11, startY + 185);
        this.mainEditBox.setMaxLength(256);
        addRenderableWidget(this.mainEditBox);
        this.setInitialFocus(this.mainEditBox);
    }

    protected void addTabButtons(Minecraft mc) {
        channelSwitchButtons.clear();

        ChannelSwitchButton globe = new ChannelSwitchButton(startX + 8, startY + 6, currentPage == CurrentPage.GLOBE, CurrentPage.GLOBE,
                Component.translatable("text.mine_chat.globe_channel"), Component.translatable("text.mine_chat.globe_tool_tip"),b -> {
            if (currentPage != CurrentPage.GLOBE) {
                mc.setScreen(new MineChatGlobeScreen());
            } else {
                boolean showRecent = MineChatConfig.DISPLAY_RECENT_MESSAGES.get();
                MineChatConfig.DISPLAY_RECENT_MESSAGES.set(!showRecent);
                MineChatConfig.CLIENT_CONFIG.save();
                if (mc.player != null) {
                    if (showRecent) {
                        mc.player.displayClientMessage(Component.translatable("text.mine_chat.recent_disabled"), false);
                    } else {
                        mc.player.displayClientMessage(Component.translatable("text.mine_chat.recent_enabled"), false);
                    }
                }
                reflashScreen();
            }
        });
        addRenderableWidget(globe);
        channelSwitchButtons.add(globe);


        ChannelSwitchButton dm = new ChannelSwitchButton(startX + 8 + 52 + 4, startY + 6, currentPage == CurrentPage.DM, CurrentPage.DM,
                Component.translatable("text.mine_chat.dm_channel"), Component.empty(),b -> {
            if (currentPage != CurrentPage.DM) {
                mc.setScreen(new MineChatDMScreen());
            } else {
                reflashScreen();
            }
        });
        addRenderableWidget(dm);
        channelSwitchButtons.add(dm);

        ChannelSwitchButton team = new ChannelSwitchButton(startX + 8 + 2 * (52 + 4), startY + 6, currentPage == CurrentPage.TEAM, CurrentPage.TEAM,
                Component.translatable("text.mine_chat.team_channel"), Component.empty(), b -> {
            if (currentPage != CurrentPage.TEAM) {
                mc.setScreen(new MineChatTeamScreen());
            } else {
                reflashScreen();
            }
        });
        addRenderableWidget(team);
        channelSwitchButtons.add(team);


        for(ChannelSwitchButton button : channelSwitchButtons) {
            button.updateUnchecked();
        }
    }

    protected void reflashScreen() {
        init();
    }

    protected void startAnimation() {
        animationStarted = true;
        animationTimer = 0;
    }

    protected void stopAnimation() {
        animationStarted = false;
        animationTimer = 0;
    }

    protected void initAnimation(){
        animationParams.clear();
        animationParams.addAll(List.of(
                new AnimationParam(0, 0, bgHeight, 0F),
                new AnimationParam(40, 0, 0, 1F)
        ));
    }


    @Override
    public void tick() {
        super.tick();
        if(this.mainEditBox != null) {
            if (this.mainEditBox.getValue().startsWith("/") && currentPage == CurrentPage.GLOBE) {
                Minecraft.getInstance().setScreen(new ChatScreen("/"));
            }
        }

        if(animationStarted){
            if(animationTimer < 20){
                animationTimer++;
            }
        }

        screenPartialTick = 0;

        List<AnimationMessage> messages = displayedMessages.stream().filter(m -> !m.isFinished()).toList();
        for(AnimationMessage message : messages){
            message.tick();
        }

        if(--updateTick < 0) {
            updateTick = maxUpdateTick;
        }

        //每隔一段时间更新一次状态
        if(updateTick == maxUpdateTick) {
            for(ChannelSwitchButton button : channelSwitchButtons) {
                button.updateUnchecked();
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        screenPartialTick = Mth.clamp(screenPartialTick + partialTick, 0, 1);
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null || mc.level == null) return;

        this.renderBackground(guiGraphics);

        PoseStack poseStack = guiGraphics.pose();

        float animationProgress = 1F;
        if(animationStarted) {
            animationProgress = Mth.clamp((animationTimer + screenPartialTick) / 2F, 0, 1);
        }
        animationProgress = (float) Mth.smoothstep(animationProgress);
        float bgAY = 0.05F * bgWidth * (1 - animationProgress);

        poseStack.pushPose();
        poseStack.translate(0, bgAY,0);

        poseStack.pushPose();
        poseStack.translate(startX, startY, 0);

        //提供渲染接口
        renderBeforeBackground(guiGraphics, poseStack, mouseX, mouseY, partialTick);

        RenderSystem.enableBlend();
        //背景
        guiGraphics.blit(background, 0, 0, 0, 0, bgWidth, bgHeight, bgWidth, bgHeight);

        RenderSystem.disableBlend();

        //提供渲染接口
        renderAfterBackground(guiGraphics, poseStack, mouseX, mouseY, partialTick);

        //提供渲染接口
        renderBeforeScissor(guiGraphics, poseStack, mouseX, mouseY, partialTick);

        //渲染文本消息区域（裁剪多余的部分）
        guiGraphics.enableScissor(messageBound.startX, (int) (messageBound.startY + bgAY), messageBound.endX, (int) (messageBound.endY + bgAY));

        //提供渲染接口
        renderBeforeMessage(guiGraphics, poseStack, mouseX, mouseY, partialTick);

        //获取所有聊天记录
        displayedMessages = getChatMessages();
        List<AnimationMessage> messages = displayedMessages;
        //清除上一帧的渲染文本内容
        renderedLines.clear();
        //根据滚动条调整显示位置
        poseStack.translate(0, scrollDelta,0);

        //每条消息之间的基础间隔
        float offsetY = 0;

        //遍历处理所有消息
        for (AnimationMessage message : messages) {
            //消息预处理，将玩家名字单独进行换行
            SenderWithMessage result = getDisplayMessage(message);
            //消息是否是本人发送的
            boolean isSender = message.getSender().equals(mc.player.getUUID());

            List<FormattedCharSequence> lines = font.split(result.finalMessage(), maxLineWidth);
            //因为换行而额外增加的间隔
            offsetY += Math.max((lines.size() - 1) * font.lineHeight, 0);

            float progress = message.getAnimationProgress(screenPartialTick);
            float pX = (isSender ? 25 : -25) * (1 - progress);
            float pY = - offsetY + baseOffsetY * (1 - progress);

            poseStack.pushPose();
            poseStack.translate(pX, pY,0);

            //绘制名字和消息
            drawMessages(guiGraphics, isSender, lines, result, progress);
            //绘制头像框
            drawFrame(guiGraphics, isSender, message, progress);

            //下一条消息的偏移量
            offsetY += baseOffsetY * progress;

            poseStack.popPose();
        }
        //记录消息的总长度
        this.totalLineHeight = offsetY;

        //结束文本渲染（防止后续内容滚动）
        poseStack.popPose();

        //提供渲染接口
        renderAfterMessage(guiGraphics, poseStack, mouseX, mouseY, partialTick);

        //裁剪完毕
        guiGraphics.disableScissor();

        //提供渲染接口
        renderAfterScissor(guiGraphics, poseStack, mouseX, mouseY, partialTick);

        //提供渲染接口
        renderBeforeRenderable(guiGraphics, poseStack, mouseX, mouseY, partialTick);

        //按钮和输入框
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        //提供渲染接口
        renderAfterRenderable(guiGraphics, poseStack, mouseX, mouseY, partialTick);

        if(messageBound.inScissorBound(mouseX, mouseY, 0, 0)) {
            //获取鼠标所在文本的样式
            Style hovered = getStyleAt(mouseX, mouseY);
            //如果存在悬浮事件，渲染悬浮文本
            if (hovered != null && hovered.getHoverEvent() != null) {
                guiGraphics.renderComponentHoverEffect(font, hovered, mouseX, mouseY);
            }
        }

        poseStack.popPose();
    }

    protected void renderBeforeBackground(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderAfterBackground(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderBeforeScissor(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderAfterScissor(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderBeforeMessage(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderAfterMessage(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderBeforeRenderable(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderAfterRenderable(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    }

    public void drawMessages(@NotNull GuiGraphics guiGraphics, boolean isSender, List<FormattedCharSequence> lines, SenderWithMessage message, float progress) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1,1,1,progress);
        //先绘制名字
        int nameX = isSender ? (384 - 5 - 22 - 4 - font.width(message.senderName()) + nameRightOffsetX) : (12 + 22 + 4 + nameLeftOffsetX);
        int nameY = 160;

        int poseX = (int) guiGraphics.pose().last().pose().m30();
        int poseY = (int) guiGraphics.pose().last().pose().m31();

        if(messageBound.inScissorBound(poseX + nameX, poseY + nameY,0, font.lineHeight)) {
            //渲染名字
            guiGraphics.drawString(font, message.senderName(), nameX, nameY, 0xFFFFFF);
            //记录
            addRenderText(message.senderName(), poseX + nameX, poseY + nameY + scrollDelta, font.width(message.senderName()));
        }

        //再绘制剩余消息
        int maxLineWidth = 0;
        for (FormattedCharSequence line : lines) {
            maxLineWidth = Math.max(maxLineWidth, font.width(line));
        }

        int lineOffset = 0;
        for (FormattedCharSequence line : lines) {
            int messageX = isSender ? (384 - 5 - 22 - 6 - maxLineWidth + messageRightOffsetX) : (12 + 22 + 6 + messageLeftOffsetX);
            int messageY = 171 + lineOffset;

            if(messageBound.inScissorBound(poseX + messageX, poseY + messageY,0,font.lineHeight)) {
                //绘制消息
                guiGraphics.drawString(font, line, messageX, messageY, 0xFFFFFF);
                //记录
                addRenderText(line, poseX + messageX, poseY + messageY, font.width(line));
            }

            lineOffset += font.lineHeight;
        }

        RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.disableBlend();
    }

    public void drawFrame(@NotNull GuiGraphics guiGraphics, boolean isSender, AnimationMessage message,float progress) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1,1,1,progress);
        //判断是否是本人发送的消息
        //如果是本人发送，消息显示在右方
        int frameX = isSender ? (384 - 22 - 2 - 5 + frameRightOffsetX) : (12 + 2 + frameLeftOffsetX);
        int headX = isSender ? (384 - 16 - 5 - 5 + frameRightOffsetX) : (12 + 5 + frameLeftOffsetX);

        int poseX = (int) guiGraphics.pose().last().pose().m30();
        int poseY = (int) guiGraphics.pose().last().pose().m31();

        if(messageBound.inScissorBound(poseX + frameX, poseY + 158, 22, 22)){
            //玩家头像
            if(message.getMessageType() != MessageType.SYSTEM){

                PlayerCache playerCache = PlayerCacheManager.getPlayerCache(message.getSender());
                if(playerCache != null){
                    //头像背景框
                    guiGraphics.blit(MineChatTextures.PLAYER_FRAME, frameX, 158, 0, 0, 22, 22, 22, 22);

                    ResourceLocation head = playerCache.getSkinLocation();
                    guiGraphics.blit(head, headX, 161, 16, 16, 16, 16, 128, 128);
                    guiGraphics.blit(head, headX, 161, 80, 16, 16, 16, 128, 128);
                }
                else{
                    //未知头像
                    guiGraphics.blit(MineChatTextures.UNKNOW, frameX, 158, 0, 0, 22, 22, 22, 22);
                }
            }
            else{
                guiGraphics.blit(MineChatTextures.SYSTEM_ICON, frameX, 158, 0, 0, 22, 22, 22, 22);
            }
        }


        RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.disableBlend();
    }

    protected void addRenderText(FormattedCharSequence text, int x, int y, int width) {
        renderedLines.add(new RenderedChatLine(text, x, y, width));
    }

    protected void addRenderText(Component text, int x, int y, int width) {
        renderedLines.add(new RenderedChatLine(text, x, y, width));
    }

    public void resetScroll(){
        this.scrollDelta = 0;
    }

    protected @NotNull MineChatScreen.SenderWithMessage getDisplayMessage(AnimationMessage message) {
        //消息
        Component text = message.getMessage();

        //确定要渲染的文本总高度
        MutableComponent senderName = Component.empty();
        MutableComponent finalMessage = Component.empty();

        //将名字单独分割出来
        List<Component> lists = text.toFlatList();
        for(int i = 0; i < lists.size(); i++){
            if(i < message.getNameLength()){
                senderName.append(lists.get(i));
            }
            else{
                finalMessage.append(lists.get(i));
            }
        }

        return new SenderWithMessage(senderName, finalMessage);
    }

    protected abstract @NotNull List<AnimationMessage> getChatMessages();

    @Nullable
    protected Style getStyleAt(int mouseX, int mouseY) {
        for (RenderedChatLine line : renderedLines) {
            if (mouseX >= line.getX()
                    && mouseX <= line.getX() + line.getWidth()
                    && mouseY >= line.getY()
                    && mouseY <= line.getY() + font.lineHeight) {

                return font.getSplitter().componentStyleAtWidth(line.getText(), mouseX - line.getX());
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Style style = getStyleAt((int) mouseX, (int) mouseY);
            if (style != null && this.handleComponentClicked(style)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void insertText(@NotNull String pText, boolean pOverwrite) {
        //根据情况跳转至私聊或者其他界面
        Minecraft mc = Minecraft.getInstance();
        if(pText.startsWith("/tell")){
            String[] text = pText.split(" ");
            mc.setScreen(new MineChatDMScreen(text[text.length - 1]));
        }
        else if(pText.startsWith("/")){
            mc.setScreen(new ChatScreen(pText));
        }
        else{
            if (pOverwrite) {
                this.mainEditBox.setValue(pText);
            }
            else {
                this.mainEditBox.insertText(pText);
            }
        }
    }


    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.mainEditBox != null && this.mainEditBox.isFocused() && this.mainEditBox.isActive()){

            if(pKeyCode == 257 || pKeyCode == 335) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    onEditBoxEnterPressed(player);
                    return true;
                }
            }
            else if(pKeyCode == 264 || pKeyCode == 265){
                return false;
            }
            if(pKeyCode == 259){
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MineChatSounds.TYPING, 1F));
            }
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    protected void onEditBoxEnterPressed(LocalPlayer player){
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 1.0F));
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if(messageBound.inScissorBound((int) pMouseX, (int) pMouseY, 0, 0)){
            pDelta = pDelta == 0 ? 0 : (pDelta < 0 ? -1 : 1);
            if (!hasShiftDown()) {
                pDelta *= 20;
            }

            if(totalLineHeight > messageBound.totalYHeight()) {
                this.scrollDelta = (int) Mth.clamp(this.scrollDelta + pDelta, 0, this.totalLineHeight - messageBound.totalYHeight());
            }
            return true;
        }
        return false;
    }

    @Override
    public void onClose(){
        super.onClose();
        stopAnimation();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public enum CurrentPage{
        GLOBE,
        DM,
        TEAM
    }

    public record AnimationParam(int timestamp, int posOffsetX, int posOffsetY, float transition) {
    }

    public record SenderWithMessage(MutableComponent senderName, MutableComponent finalMessage) {
    }

    public static class RenderedChatLine{
        private final FormattedCharSequence text;
        private final int x;
        private final int y;
        private final int width;

        RenderedChatLine(Component text, int x, int y, int width) {
            this.text = text.getVisualOrderText();
            this.x = x;
            this.y = y;
            this.width = width;
        }

        RenderedChatLine(FormattedCharSequence text, int x, int y, int width) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
        }

        public FormattedCharSequence getText() {
            return text;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }
    }

    public static class ScissorBound{
        private int startX;
        private int startY;
        private int endX;
        private int endY;

        public ScissorBound(int startX, int startY, int endX, int endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }

        public boolean inScissorBound(int posX, int posY, int width, int height) {
            return (posX >= startX && posX <= endX || (posX + width) >= startX && (posX + width) <= endX)&&
                    (posY >= startY && posY <= endY ||  (posY + height) >= startY && (posY + height) <= endY);
        }

        public int totalXHeight(){
            return endX - startX;
        }

        public int totalYHeight(){
            return endY - startY;
        }

        public int getStartX() {
            return startX;
        }

        public void setStartX(int startX) {
            this.startX = startX;
        }

        public int getStartY() {
            return startY;
        }

        public void setStartY(int startY) {
            this.startY = startY;
        }

        public int getEndX() {
            return endX;
        }

        public void setEndX(int endX) {
            this.endX = endX;
        }

        public int getEndY() {
            return endY;
        }

        public void setEndY(int endY) {
            this.endY = endY;
        }
    }
}
