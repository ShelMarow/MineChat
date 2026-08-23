package net.shelmarow.mine_chat.chat.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.message.AnimationMessage;
import net.shelmarow.mine_chat.chat.message.ChatMessage;
import net.shelmarow.mine_chat.chat.message.chat_enum.MessageType;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import net.shelmarow.mine_chat.chat.picture.data.ChatPicture;
import net.shelmarow.mine_chat.chat.playercache.PlayerCache;
import net.shelmarow.mine_chat.chat.playercache.PlayerCacheManager;
import net.shelmarow.mine_chat.chat.screen.button.ChannelSwitchButton;
import net.shelmarow.mine_chat.chat.screen.button.ChatPictureButton;
import net.shelmarow.mine_chat.chat.screen.button.MButton;
import net.shelmarow.mine_chat.chat.screen.button.SendPictureButton;
import net.shelmarow.mine_chat.chat.screen.editbox.MineChatCommonEditBox;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.SenderType;
import net.shelmarow.mine_chat.chat.texture.MineChatTextures;
import net.shelmarow.mine_chat.config.MineChatClientConfig;
import net.shelmarow.mine_chat.network.MineChatNetwork;
import net.shelmarow.mine_chat.network.packet.client.C2SCheckPicturePacket;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public abstract class MineChatScreen extends Screen {

    public static final int LIMIT_WIDTH = 80;
    public static final int LIMIT_HEIGHT = 80;
    public static final int SYSTEM_LIMIT_WIDTH = 160;
    public static final int SYSTEM_LIMIT_HEIGHT = 90;
    protected static final int PICTURE_PER_PAGE = 6;
    //GUI动画参数
    protected static boolean animationStarted = false;
    protected static float animationTotalTime = 4;
    protected static float animationTimer = 0;
    protected static List<AnimationParam> animationParams = new ArrayList<>();
    protected final List<RenderedChatLine> renderedLines = new ArrayList<>();
    protected final List<ChannelSwitchButton> channelSwitchButtons = new ArrayList<>();
    protected final List<AbstractWidget> pictureButtons = new ArrayList<>();

    protected ResourceLocation background = MineChatTextures.COMMON_CHANNEL;
    protected CurrentPage currentPage = CurrentPage.GLOBE;
    protected @Nullable EditBox mainEditBox;
    //每隔一定时间更新，减少负载
    protected int maxUpdateTick = 2;
    protected int updateTick = 0;
    //滚动设置
    protected float scrollDelta = 0;
    protected float totalLineHeight = 0;
    private boolean draggingMessageScrollBar = false;
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
    //显示的消息
    protected float screenPartialTick = 0;
    protected List<AnimationMessage> displayedMessages = new ArrayList<>();
    //表情包
    protected boolean showPicturePanel = false;
    protected boolean customPicture = false;
    protected int picturePage = 0;
    protected List<RenderedChatPicture> renderedChatPictures = new ArrayList<>();
    protected @Nullable String currentPictureGroup = null;
    protected final List<AbstractWidget> pictureGroupButtons = new ArrayList<>();

    //全屏显示图片
    protected boolean displayingPicture = false;
    protected ChatPicture displayedPicture = null;

    //过渡动画
    protected int displayPictureTick = 0;
    protected int maxDisplayPictureTick = 10;
    protected float pictureZoomProgress = 0f;
    protected int pictureStartX = 0;
    protected int pictureStartY = 0;
    protected float pictureStartWidth = 0;
    protected float pictureStartHeight = 0;
    protected boolean isPictureZoomingOut = false;

    //图片缩放
    protected float pictureZoomScale = 1.0f;
    protected float pictureZoomTargetScale = 1.0f;
    protected float pictureZoomOffsetX = 0f;
    protected float pictureZoomOffsetY = 0f;

    //图片移动
    protected boolean isDragging = false;
    protected boolean isMouseDown = false;
    protected boolean isClick = false;

    //历史消息
    protected int historyPos = -1;
    protected String historyBuffer = "";
    protected SendPictureButton pictureButton;

    //@功能
    protected final List<MessageRenderInfo> messageRenderInfos = new ArrayList<>();
    protected MButton jumpToMentionButton;
    protected boolean hasUnreadMention = false;
    protected final Map<AnimationMessage, Integer> highlightTimers = new HashMap<>();
    protected static final int HIGHLIGHT_DURATION = 40;
    protected List<String> mentionSuggestions = new ArrayList<>();
    protected int mentionSuggestionIndex = 0;
    protected boolean isMentionCompleting = false;


    public MineChatScreen() {
        super(Component.empty());
        initAnimation();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            this.guiOpenTime = mc.level.getGameTime();
        }
        if (!animationStarted) {
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

    protected void initRenderOffset() {
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

        addPictureButton();
        addJumpToMentionButton();
    }

    protected void addJumpToMentionButton() {
        jumpToMentionButton = new MButton(startX + bgWidth / 2 - 40, startY + 30, 80, 20,
                Component.translatable("text.mine_chat.mention_you").withStyle(ChatFormatting.RED), b -> jumpToLatestMention());
        jumpToMentionButton.visible = false;
        jumpToMentionButton.active = false;
        addRenderableWidget(jumpToMentionButton);
    }

    protected void jumpToLatestMention() {
        MessageRenderInfo targetInfo = null;
        for (int i = messageRenderInfos.size() - 1; i >= 0; i--) {
            MessageRenderInfo info = messageRenderInfos.get(i);
            if (info.message().isHasMention() && !info.message().isMentionRead()) {
                targetInfo = info;
                break;
            }
        }

        if (targetInfo == null) return;

        this.scrollDelta = (int) Mth.clamp(-targetInfo.y,0 ,this.totalLineHeight - messageBound.totalYHeight());
    }

    protected void updateJumpButtonVisibility() {
        hasUnreadMention = false;
        for (MessageRenderInfo info : messageRenderInfos) {
            AnimationMessage msg = info.message();
            if (msg.isHasMention() && !msg.isMentionRead()) {
                int screenY = (int) (info.y + scrollDelta);
                int screenX = info.x;

                boolean isVisible = messageBound.inScissorBound(info.x, info.y + 30, info.width, info.height);

                if (!isVisible) {
                    hasUnreadMention = true;
                    break;
                }
            }
        }

        if (jumpToMentionButton != null) {
            jumpToMentionButton.visible = hasUnreadMention;
            jumpToMentionButton.active = hasUnreadMention;
        }
    }


    protected void addPictureButton() {
        //添加表情包按钮
        pictureButton = new SendPictureButton(startX + 11 + 378 - 24, startY + 185, Component.empty(), b -> {
            showPicturePanel = !showPicturePanel;

            if (showPicturePanel) {
                picturePage = 0;
                currentPictureGroup = null;
                createPictureButtons(picturePage, customPicture);
            } else {
                clearPictureButtons();
            }
        }, b -> {
            if (MineChatManager.isServerInstalled()) {
                if (showPicturePanel) {
                    picturePage = 0;

                    // 切换普通表情 / 自定义表情
                    customPicture = !customPicture;

                    // 切换类型后重新从“全部”开始
                    currentPictureGroup = null;

                    createPictureButtons(picturePage, customPicture);
                } else {
                    clearPictureButtons();
                }
            } else {
                if(showPicturePanel){
                    showPicturePanel = false;
                    clearPictureButtons();
                }
                if (getMinecraft().player != null) {
                    getMinecraft().player.displayClientMessage(Component.translatable("text.mine_chat.server_not_installed"), false);
                }
            }
        });

        if (showPicturePanel) {
            createPictureButtons(picturePage, customPicture);
        } else {
            clearPictureButtons();
        }

        addRenderableWidget(pictureButton);
    }

    protected void createPictureButtons(int page, boolean customPicture) {
        clearPictureButtons();

        picturePage = page;
        this.customPicture = customPicture;

        ClientPictureManager manager = ClientPictureManager.getInstance();
        Map<String, ChatPicture> pictureMap;

        if (customPicture) {
            pictureMap = manager.getCustomPictures();
        }
        else {
            if (currentPictureGroup == null) {
                pictureMap = manager.getPictures();
            }
            else {
                Map<String, Map<String, ChatPicture>> groups = manager.getPictureGroups();
                pictureMap = groups.getOrDefault(currentPictureGroup, Collections.emptyMap());
            }
        }

        ArrayList<Map.Entry<String, ChatPicture>> list = new ArrayList<>(pictureMap.entrySet());

        int sidePadding = 25;
        int topPadding = 20;
        int bottomPadding = customPicture ? 5 : 25;
        int buttonSize = customPicture ? 36 : 28;
        int buttonSpacing = 2;
        int cellSize = buttonSize + buttonSpacing;

        int pictureAreaX = messageBound.getStartX() + sidePadding;
        int pictureAreaY = messageBound.getStartY() + messageBound.getHeight() / 4 + topPadding;
        int pictureAreaWidth = Math.max(buttonSize, messageBound.getWidth() - sidePadding * 2);
        int pictureAreaHeight = Math.max(buttonSize, messageBound.getHeight() * 3 / 4 - topPadding - bottomPadding);

        int columns = Math.max(1, (pictureAreaWidth + buttonSpacing) / cellSize);
        int rows = Math.max(1, (pictureAreaHeight + buttonSpacing) / cellSize);
        int picturePerPage = columns * rows;


        int extraButtonCount = customPicture ? 1 : 0;
        int totalItemCount = list.size() + extraButtonCount;
        int totalPage = totalItemCount == 0 ? 1 : (totalItemCount + picturePerPage - 1) / picturePerPage;
        picturePage = Mth.clamp(picturePage, 0, Math.max(0, totalPage - 1));

        int start = picturePage * picturePerPage;
        int end = Math.min(start + picturePerPage, totalItemCount);


        int index = 0;
        for (int i = start; i < end; i++) {
            int column = index % columns;
            int row = index / columns;
            int x = pictureAreaX + column * cellSize;
            int y = pictureAreaY + row * cellSize;

            if (customPicture && i == 0) {
                MButton addCustomPictureButton = new MButton(x + 4, y + 4, buttonSize - 8, buttonSize - 8,
                        Component.literal("+"),
                        b -> Minecraft.getInstance().setScreen(new CustomPictureConfigScreen(this)));
                addRenderableWidget(addCustomPictureButton);
                pictureButtons.add(addCustomPictureButton);
                index++;
                continue;
            }


            int pictureIndex = customPicture ? i - 1 : i;
            if (pictureIndex < 0 || pictureIndex >= list.size()) {
                continue;
            }

            Map.Entry<String, ChatPicture> entry = list.get(pictureIndex);
            String pictureName = entry.getKey();
            ChatPicture picture = entry.getValue();


            ChatPictureButton button = new ChatPictureButton(x, y, buttonSize, buttonSize, picture, b -> {
                sendPicture(pictureName, customPicture);
                if (customPicture && MineChatManager.isServerInstalled()) {
                    MineChatNetwork.sendToServer(new C2SCheckPicturePacket(pictureName));
                }
                showPicturePanel = false;
                clearPictureButtons();
            });
            addRenderableWidget(button);
            pictureButtons.add(button);
            index++;
        }

        if (!customPicture) {
            createPictureGroupButtons();
        }


        int pageButtonWidth = 15;
        int pageButtonHeight = 32;

        int pictureCenterY = pictureAreaY + pictureAreaHeight / 2;
        int pageButtonY = pictureCenterY - pageButtonHeight / 2;


        MButton picturePrevButton = new MButton(messageBound.getStartX() + 5, pageButtonY, pageButtonWidth, pageButtonHeight, Component.literal("<"), b -> {
            if (picturePage > 0) {
                picturePage--;
                createPictureButtons(picturePage, this.customPicture);
            }
        });
        addRenderableWidget(picturePrevButton);
        pictureButtons.add(picturePrevButton);

        MButton pictureNextButton = new MButton(messageBound.getEndX() - 5 - pageButtonWidth, pageButtonY, pageButtonWidth, pageButtonHeight, Component.literal(">"), b -> {
            if (picturePage < totalPage - 1) {
                picturePage++;
                createPictureButtons(picturePage, this.customPicture);
            }
        });
        addRenderableWidget(pictureNextButton);
        pictureButtons.add(pictureNextButton);

        picturePrevButton.active = picturePage > 0;
        pictureNextButton.active = picturePage < totalPage - 1;
    }

    protected void createPictureGroupButtons() {
        clearPictureGroupButtons();

        ClientPictureManager instance = ClientPictureManager.getInstance();

        Map<String, Map<String, ChatPicture>> groups = instance.getPictureGroups();

        int buttonWidth = 42;
        int buttonHeight = 16;
        int buttonSpacing = 3;

        int startX = messageBound.getStartX() + 20;
        int columns = 7;

        int totalButtons = groups.size() + 1;
        int rows = (totalButtons + columns - 1) / columns;
        int bottomPadding = 10;
        int groupAreaHeight = rows * buttonHeight + (rows - 1) * buttonSpacing;

        int y = messageBound.getEndY() - bottomPadding - groupAreaHeight;

        MButton allButton = new MButton(startX, y, buttonWidth, buttonHeight, Component.literal("ALL"), b -> {
            if (currentPictureGroup != null) {
                currentPictureGroup = null;
                picturePage = 0;
                createPictureButtons(picturePage, false);
            }
        });

        allButton.active = currentPictureGroup != null;

        addRenderableWidget(allButton);
        pictureGroupButtons.add(allButton);

        int index = 1;

        for (String group : groups.keySet()) {
            int column = index % columns;
            int row = index / columns;
            int x = startX + column * (buttonWidth + buttonSpacing);
            int buttonY = y + row * (buttonHeight + buttonSpacing);
            String displayName = getPictureGroupDisplayName(group);
            MButton groupButton = new MButton(x, buttonY, buttonWidth, buttonHeight, Component.translatable("text.mine_chat.picture_group." + displayName), b -> {
                currentPictureGroup = group;
                picturePage = 0;
                createPictureButtons(picturePage, false);
            });
            groupButton.active = !Objects.equals(currentPictureGroup, group);
            addRenderableWidget(groupButton);
            pictureGroupButtons.add(groupButton);

            index++;
        }
    }

    protected void clearPictureGroupButtons() {
        for (AbstractWidget button : pictureGroupButtons) {
            removeWidget(button);
        }

        pictureGroupButtons.clear();
    }

    protected String getPictureGroupDisplayName(String group) {
        if (group == null || group.isEmpty()) {
            return "Unknow";
        }

        return group;
    }

    protected void sendPicture(String name, boolean networkPicture) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || mainEditBox == null) return;
        mainEditBox.setValue("<MineChatPicture:[\"" + name + (networkPicture ? "|network" : "|chat") + "\"]>");
        onEditBoxEnterPressed(player);
    }

    protected void clearPictureButtons() {
        for (AbstractWidget button : pictureButtons) {
            removeWidget(button);
        }

        pictureButtons.clear();
        clearPictureGroupButtons();

        picturePage = 0;
    }

    protected void addTabButtons(Minecraft mc) {
        channelSwitchButtons.clear();

        ChannelSwitchButton globe = new ChannelSwitchButton(startX + 8, startY + 6, currentPage == CurrentPage.GLOBE, CurrentPage.GLOBE, Component.translatable("text.mine_chat.globe_channel"), Component.translatable("text.mine_chat.globe_tool_tip"), b -> {
            if (currentPage != CurrentPage.GLOBE) {
                mc.setScreen(new MineChatGlobeScreen());
            } else {
                boolean showRecent = MineChatClientConfig.DISPLAY_RECENT_MESSAGES.get();
                MineChatClientConfig.DISPLAY_RECENT_MESSAGES.set(!showRecent);
                MineChatClientConfig.CLIENT_CONFIG.save();
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


        ChannelSwitchButton dm = new ChannelSwitchButton(startX + 8 + 52 + 4, startY + 6, currentPage == CurrentPage.DM, CurrentPage.DM, Component.translatable("text.mine_chat.dm_channel"), Component.empty(), b -> {
            if (currentPage != CurrentPage.DM) {
                mc.setScreen(new MineChatDMScreen());
            } else {
                reflashScreen();
            }
        });
        addRenderableWidget(dm);
        channelSwitchButtons.add(dm);

        ChannelSwitchButton team = new ChannelSwitchButton(startX + 8 + 2 * (52 + 4), startY + 6, currentPage == CurrentPage.TEAM, CurrentPage.TEAM, Component.translatable("text.mine_chat.team_channel"), Component.empty(), b -> {
            if (currentPage != CurrentPage.TEAM) {
                mc.setScreen(new MineChatTeamScreen());
            } else {
                reflashScreen();
            }
        });
        addRenderableWidget(team);
        channelSwitchButtons.add(team);


        for (ChannelSwitchButton button : channelSwitchButtons) {
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

    protected void initAnimation() {
        animationParams.clear();
        animationParams.addAll(List.of(new AnimationParam(0, 0, bgHeight, 0F), new AnimationParam(40, 0, 0, 1F)));
    }

    @Override
    public void onFilesDrop(@NotNull List<Path> paths) {
        ClientPictureManager.getInstance().loadDropFiles(paths);
        if(showPicturePanel){
            showPicturePanel = false;
            reflashScreen();
        }
    }

    @Override
    public void tick() {
        super.tick();

        screenPartialTick = 0;

        if (this.mainEditBox != null) {
            if (this.mainEditBox.getValue().startsWith("/") && currentPage == CurrentPage.GLOBE) {
                Minecraft.getInstance().setScreen(new ChatScreen(this.mainEditBox.getValue()));
                stopAnimation();
            }

            if(this.mainEditBox instanceof MineChatCommonEditBox chatCommonEditBox){
                chatCommonEditBox.updateMentionSuggestions();
            }
        }

        if (animationStarted) {
            if (animationTimer < animationTotalTime) {
                animationTimer++;
            }
        }

        // 处理图片动画 - 进入和退出共用 displayPictureTick
        if (displayingPicture) {
            // 进入动画：tick 从 0 增加到 max
            if (!isPictureZoomingOut && displayPictureTick < maxDisplayPictureTick) {
                displayPictureTick++;
            }
            // 退出动画：tick 从 max 减少到 0
            if (isPictureZoomingOut && displayPictureTick > 0) {
                displayPictureTick--;
                if (displayPictureTick == 0) {
                    // 退出动画完成，清理状态
                    isPictureZoomingOut = false;
                    displayingPicture = false;
                    displayedPicture = null;
                    pictureZoomProgress = 0;
                }
            }
        } else {
            if (displayPictureTick > 0 && !isPictureZoomingOut) {
                displayPictureTick = 0;
            }
        }

        // 更新高亮计时器
        Iterator<Map.Entry<AnimationMessage, Integer>> iterator = highlightTimers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<AnimationMessage, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }

        List<AnimationMessage> messages = displayedMessages.stream().filter(m -> !m.isFinished()).toList();
        for (AnimationMessage message : messages) {
            message.tick();
        }

        if (--updateTick < 0) {
            updateTick = maxUpdateTick;
        }

        //每隔一段时间更新一次状态
        if (updateTick == maxUpdateTick) {
            for (ChannelSwitchButton button : channelSwitchButtons) {
                button.updateUnchecked();
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        this.renderBackground(guiGraphics);

        screenPartialTick = Mth.clamp(screenPartialTick + partialTick, 0, 1);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        PoseStack poseStack = guiGraphics.pose();

        float animationProgress = 1F;
        if (animationStarted) {
            animationProgress = Mth.clamp((animationTimer + screenPartialTick) / animationTotalTime, 0, 1);
        }
        animationProgress = (float) Mth.smoothstep(animationProgress);
        float bgAY = 0.05F * bgWidth * (1 - animationProgress);


        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, animationProgress);

        poseStack.pushPose();
        poseStack.translate(0, bgAY, 0);


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

        //获取所有聊天记录
        displayedMessages = getChatMessages();
        List<AnimationMessage> messages = displayedMessages;
        //清除上一帧的渲染文本内容
        renderedLines.clear();
        renderedChatPictures.clear();
        //根据滚动条调整显示位置
        poseStack.translate(0, scrollDelta, 0);

        //提供渲染接口
        renderBeforeMessage(guiGraphics, poseStack, mouseX, mouseY, partialTick);

        //每条消息之间的基础间隔
        float messageOffsetY = 0;

        //遍历处理所有消息
        for (AnimationMessage message : messages) {
            //消息预处理，将玩家名字单独进行换行
            ChatMessage.SenderWithMessage result = message.getDisplayMessage();
            //消息是否是本人发送的
            boolean isSender = message.getSender().getUuid().equals(mc.player.getUUID());

            ChatPicture picture = null;
            Vec2 pictureSize = new Vec2(LIMIT_WIDTH, LIMIT_HEIGHT);
            List<FormattedCharSequence> lines;
            ClientPictureManager instance = ClientPictureManager.getInstance();
            if (instance.isPicture(result.finalMessage().getString())) {
                picture = instance.getPicture(instance.getPictureID(result.finalMessage().getString()));
                if (picture != null) {
                    pictureSize = picture.getDisplaySize(picture.isSystem() ? SYSTEM_LIMIT_WIDTH : LIMIT_WIDTH, picture.isSystem() ? SYSTEM_LIMIT_HEIGHT : LIMIT_HEIGHT);
                } else {
                    if (instance.getPictureType(instance.getPictureID(result.finalMessage().getString())).equals("network")) {
                        result = new ChatMessage.SenderWithMessage(result.senderName(), Component.translatable("text.mine_chat.picture_loading"));
                    } else {
                        result = new ChatMessage.SenderWithMessage(result.senderName(), Component.translatable("text.mine_chat.unknow_picture"));
                    }
                }
                lines = font.split(result.finalMessage(), Integer.MAX_VALUE);
            }
            else {
                lines = font.split(result.finalMessage(), maxLineWidth);
            }

            int msgHeight = Math.max((lines.size() - 1) * font.lineHeight, 0);
            float sendProgress = message.getAnimationProgress(screenPartialTick);
            float changedProgress = message.getChangedProgress(screenPartialTick);
            messageOffsetY += picture == null ? ((float) msgHeight) * changedProgress * sendProgress : (int) pictureSize.y * changedProgress * sendProgress;

            float pX = (isSender ? 25 : -25) * (1 - sendProgress);
            float pY = -messageOffsetY + (baseOffsetY) * (1 - sendProgress);

            int msgWidth = 0;
            for (FormattedCharSequence line : lines) {
                msgWidth = Math.max(msgWidth, font.width(line));
            }

            messageRenderInfos.add(new MessageRenderInfo(message, 0, (int) pY, msgWidth, msgHeight));

            poseStack.pushPose();
            poseStack.translate(pX, pY, 0);

            //绘制名字和消息
            drawMessages(guiGraphics, isSender, lines, result, picture, sendProgress * animationProgress, changedProgress + 0.5F, message);
            //绘制头像框
            drawFrame(guiGraphics, isSender, message, sendProgress * animationProgress);

            //下一条消息的偏移量
            messageOffsetY += baseOffsetY * sendProgress;

            poseStack.popPose();
        }

        //记录消息的总长度
        this.totalLineHeight = messageOffsetY;

        //提供渲染接口
        renderAfterMessage(guiGraphics, poseStack, mouseX, mouseY, partialTick);

        //结束文本渲染（防止后续内容滚动）
        poseStack.popPose();

        //裁剪完毕
        guiGraphics.disableScissor();

        //提供渲染接口
        renderAfterScissor(guiGraphics, poseStack, mouseX, mouseY, partialTick);

        //提供渲染接口
        renderBeforeRenderable(guiGraphics, poseStack, mouseX, mouseY, partialTick);


        poseStack.pushPose();
        poseStack.translate(0, 0, 1);

        RenderSystem.enableBlend();
        //绘制表情包选择背景
        if(showPicturePanel){
            guiGraphics.blit(
                    MineChatTextures.CHAT_RECENT_MESSAGE,
                    messageBound.getStartX(),
                    (int) (messageBound.getStartY() + Math.ceil(messageBound.getHeight() / 4F)),
                    0,
                    0,
                    messageBound.getWidth(),
                    messageBound.getHeight() * 3 / 4,
                    messageBound.getWidth(),
                    messageBound.getHeight() * 3 / 4
            );

            MutableComponent translatable;
            if(customPicture){
                translatable = Component.translatable("text.mine_chat.custom_emoji");
            }
            else {
                translatable = Component.translatable("text.mine_chat.common_emoji");
            }
            guiGraphics.drawString(
                    font, translatable.withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA),
                     startX + bgWidth / 2 + (bgWidth - messageBound.getWidth()) / 2 - 12 - font.width(translatable) / 2,
                    messageBound.endY - messageBound.getHeight() * 3 / 4 + 10,0xFFFFFFFF);

        }

        RenderSystem.disableBlend();

        //按钮和输入框
        for (Renderable renderable : this.renderables) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, animationProgress);
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }

        poseStack.popPose();

        //提供渲染接口
        renderAfterRenderable(guiGraphics, poseStack, mouseX, mouseY, partialTick);

        if (messageBound.inScissorBound(mouseX, mouseY, 0, 0)) {
            //获取鼠标所在文本的样式
            Style hovered = getStyleAt(mouseX, mouseY);
            //如果存在悬浮事件，渲染悬浮文本
            if (hovered != null && hovered.getHoverEvent() != null) {
                guiGraphics.renderComponentHoverEffect(font, hovered, mouseX, mouseY);
            }
        }
        //guiGraphics.drawString(mc.font,mouseX+" "+mouseY,0,0,0xFFFFFFFF);


        renderMessageScrollBar(guiGraphics, scrollDelta);

        poseStack.popPose();

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        //渲染全屏图片

        renderFullPicture(guiGraphics, poseStack);


        updateJumpButtonVisibility();
    }

    private void renderFullPicture(@NotNull GuiGraphics guiGraphics, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.translate(0, 0, 2);

        if (displayPictureTick > 0 && displayedPicture != null) {
            // 计算进度
            float progress = Mth.clamp((displayPictureTick + (isPictureZoomingOut ? -screenPartialTick : screenPartialTick)) / maxDisplayPictureTick, 0, 1);

            float displayProgress = isPictureZoomingOut ? progress : (float) (1 - Math.pow(1 - progress, 3));
            float displayAlpha = isPictureZoomingOut ? progress : 1.0f;

            // 计算目标位置（全屏）
            Vec2 targetSize = displayedPicture.getDisplaySize(width, height);
            float targetX = (width - targetSize.x) / 2;
            float targetY = (height - targetSize.y) / 2;

            // 插值计算当前位置和大小
            float currentX = pictureStartX + (targetX - pictureStartX) * displayProgress;
            float currentY = pictureStartY + (targetY - pictureStartY) * displayProgress;
            float currentWidth = pictureStartWidth + (targetSize.x - pictureStartWidth) * displayProgress;
            float currentHeight = pictureStartHeight + (targetSize.y - pictureStartHeight) * displayProgress;

            // 应用缩放
            float scale = isPictureZoomingOut ? 1.0f : pictureZoomScale;
            float scaledWidth = currentWidth * scale;
            float scaledHeight = currentHeight * scale;

            // 计算图片中心
            float centerX = currentX + currentWidth / 2;
            float centerY = currentY + currentHeight / 2;

            // 应用偏移
            float offsetX = isPictureZoomingOut ? 0 : pictureZoomOffsetX;
            float offsetY = isPictureZoomingOut ? 0 : pictureZoomOffsetY;

            float renderX = centerX - scaledWidth / 2 + offsetX;
            float renderY = centerY - scaledHeight / 2 + offsetY;

            // 背景遮罩透明度
            int alpha = (int) (0xAA * displayProgress * displayAlpha);
            int bgColor = (alpha << 24);
            guiGraphics.fill(0, 0, width, height, bgColor);

            // 获取纹理
            ResourceLocation texture;
            if (displayedPicture.isGif()) {
                displayedPicture.updateGif();
                texture = displayedPicture.getGifTexture();
            } else {
                texture = displayedPicture.getTexture();
            }

            // 设置透明度
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, displayAlpha);

            // 绘制图片
            guiGraphics.blit(texture, (int) renderX, (int) renderY, 0, 0, (int) scaledWidth, (int) scaledHeight, (int) scaledWidth, (int) scaledHeight);

            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.disableBlend();

            // 当动画完成且不是退出状态时，添加提示
            if (progress >= 1 && !isPictureZoomingOut) {
//                String hint = "滚轮缩放 · 拖动平移 · 点击关闭";
//                int hintWidth = font.width(hint);
//                guiGraphics.drawString(font, hint, (width - hintWidth) / 2, height - 20, 0xCCFFFFFF);

                // 显示当前缩放比例
                String zoomText = String.format("%.0f%%", pictureZoomScale * 100);
                int zoomWidth = font.width(zoomText);
                guiGraphics.drawString(font, zoomText, width - zoomWidth, 0, 0x88FFFFFF);
            }
        }

        poseStack.pushPose();
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


    protected void renderMessageScrollBar(GuiGraphics guiGraphics, float scrollOffset) {
        float contentHeight = totalLineHeight;
        if (messageBound == null) {
            return;
        }

        int boundX = messageBound.getStartX();
        int boundY = messageBound.getStartY();

        int boundWidth = messageBound.totalXHeight();
        int boundHeight = messageBound.totalYHeight();

        if (contentHeight <= boundHeight || boundHeight <= 0) {
            return;
        }
        int barWidth = 3;
        int rightOffset = 2;
        int barX = boundX + boundWidth - barWidth - rightOffset;
        int thumbHeight = Math.round(boundHeight * boundHeight / contentHeight);

        thumbHeight = Math.max(10, Math.min(boundHeight, thumbHeight));
        float maxScroll = Math.max(0, contentHeight - boundHeight);

        float progress;
        if (maxScroll <= 0) {
            progress = 0;
        } else {
            progress = scrollOffset / maxScroll;
        }
        progress = 1 - Math.max(0, Math.min(1, progress));

        int maxThumbOffset = boundHeight - thumbHeight;
        int thumbY = boundY + Math.round(maxThumbOffset * progress);

        guiGraphics.fill(barX, boundY, barX + barWidth, boundY + boundHeight, 0x55000000);
        guiGraphics.fill(barX, thumbY, barX + barWidth, thumbY + thumbHeight, 0xFFFFFFFF);
    }

    private boolean isMouseOverMessageScrollBarThumb(double mouseX, double mouseY) {
        if (messageBound == null) {
            return false;
        }

        float contentHeight = totalLineHeight;

        int boundX = messageBound.getStartX();
        int boundY = messageBound.getStartY();

        int boundWidth = messageBound.totalXHeight();
        int boundHeight = messageBound.totalYHeight();

        if (contentHeight <= boundHeight || boundHeight <= 0) {
            return false;
        }

        int barWidth = 3;
        int rightOffset = 2;

        int barX = boundX + boundWidth - barWidth - rightOffset;

        int thumbHeight = Math.round(boundHeight * boundHeight / contentHeight);

        thumbHeight = Math.max(10, Math.min(boundHeight, thumbHeight));

        float maxScroll = Math.max(0, contentHeight - boundHeight);

        if (maxScroll <= 0) {
            return false;
        }

        float progress = scrollDelta / maxScroll;

        progress = 1 - Math.max(0, Math.min(1, progress));

        int maxThumbOffset = boundHeight - thumbHeight;

        int thumbY = boundY + Math.round(maxThumbOffset * progress);

        return mouseX >= barX && mouseX <= barX + barWidth && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
    }

    public void drawMessages(@NotNull GuiGraphics guiGraphics, boolean isSender, List<FormattedCharSequence> lines, ChatMessage.SenderWithMessage message, @Nullable ChatPicture picture, float progress, float changedProgress, AnimationMessage animationMessage) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, Math.max(progress, 0.1F));
        //先绘制名字
        int nameX = isSender ? (384 - 5 - 22 - 4 - font.width(message.senderName()) + nameRightOffsetX) : (12 + 22 + 4 + nameLeftOffsetX);
        int nameY = 160;

        int poseX = (int) guiGraphics.pose().last().pose().m30();
        int poseY = (int) guiGraphics.pose().last().pose().m31();

        if (messageBound.inScissorBound(poseX + nameX, poseY + nameY, 0, font.lineHeight)) {
            //渲染名字
            guiGraphics.drawString(font, message.senderName(), nameX, nameY, 0xFFFFFF);
            //记录
            addRenderText(message.senderName(), poseX + nameX, poseY + nameY, font.width(message.senderName()));
        }

        if (picture != null) {
            Vec2 pictureSize = picture.getDisplaySize(picture.isSystem() ? SYSTEM_LIMIT_WIDTH : LIMIT_WIDTH, picture.isSystem() ? SYSTEM_LIMIT_HEIGHT : LIMIT_HEIGHT);
            int messageX = isSender ? (int) (384 - 5 - 22 - 6 - pictureSize.x + messageRightOffsetX) : (12 + 22 + 6 + messageLeftOffsetX);
            int messageY = 171;

            if (messageBound.inScissorBound(poseX + messageX, poseY + messageY - 5, (int) pictureSize.x, (int) pictureSize.y)) {
                if (picture.isGif()) {
                    picture.updateGif();
                    guiGraphics.blit(picture.getGifTexture(), messageX, messageY, 0, 0, (int) pictureSize.x, (int) pictureSize.y, (int) pictureSize.x, (int) pictureSize.y);
                } else {
                    guiGraphics.blit(picture.getTexture(), messageX, messageY, 0, 0, (int) pictureSize.x, (int) pictureSize.y, (int) pictureSize.x, (int) pictureSize.y);
                }
                renderedChatPictures.add(new RenderedChatPicture(picture, poseX + messageX, poseY + messageY));
            }
        } else{
            RenderSystem.setShaderColor(1, 1, 1, Math.max(progress * changedProgress, 0.1F));
            List<AnimationMessage.MentionInfo> mentions = animationMessage.getMentions();

            int maxLineWidth = 0;
            for (FormattedCharSequence line : lines) {
                maxLineWidth = Math.max(maxLineWidth, font.width(line));
            }

            int lineOffset = 0;
            int messageX = isSender ? (384 - 5 - 22 - 6 - maxLineWidth + messageRightOffsetX) : (12 + 22 + 6 + messageLeftOffsetX);

                for (FormattedCharSequence line : lines) {
                    int messageY = 171 + lineOffset;
                    if (messageBound.inScissorBound(poseX + messageX, poseY + messageY - 5, 0, font.lineHeight)) {
                        if (animationMessage.isHasMention() && !animationMessage.isMentionRead()) {
                            MineChatManager.checkPingMessage();
                            animationMessage.setMentionRead(true);
                            highlightTimers.put(animationMessage, HIGHLIGHT_DURATION);
                        }

                        if (animationMessage.isHasMention() && highlightTimers.containsKey(animationMessage)) {
                            int remaining = highlightTimers.get(animationMessage);
                            if (remaining > 0) {
                                float fadeAlpha = Math.min(1.0f, remaining / 20.0f);
                                int alpha = (int) (0x88 * fadeAlpha);
                                int highlightColor = (alpha << 24) | 0x5555FF;
                                guiGraphics.fill(messageX, messageY, messageX + font.width(line), messageY + font.lineHeight, highlightColor);
                            } else {
                                highlightTimers.remove(animationMessage);
                            }
                        }

                        guiGraphics.drawString(font, line, messageX, messageY, 0xFFFFFF);
                        addRenderText(line, poseX + messageX, poseY + messageY, font.width(line));
                    }
                    lineOffset += font.lineHeight;
                }

        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    public void drawFrame(@NotNull GuiGraphics guiGraphics, boolean isSender, AnimationMessage message, float progress) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, progress);
        //判断是否是本人发送的消息
        //如果是本人发送，消息显示在右方
        int frameX = isSender ? (384 - 22 - 2 - 5 + frameRightOffsetX) : (12 + 2 + frameLeftOffsetX);
        int headX = isSender ? (384 - 16 - 5 - 5 + frameRightOffsetX) : (12 + 5 + frameLeftOffsetX);

        int poseX = (int) guiGraphics.pose().last().pose().m30();
        int poseY = (int) guiGraphics.pose().last().pose().m31();

        if (messageBound.inScissorBound(poseX + frameX, poseY + 158, 22, 22)) {
            //玩家头像
            if (message.getMessageType() != MessageType.SYSTEM) {
                ChatSender sender = message.getSender();
                if (sender.getSenderType() == SenderType.PLAYER) {
                    PlayerCache playerCache = PlayerCacheManager.getPlayerCache(sender.getUuid());
                    if (playerCache != null) {
                        //头像背景框
                        guiGraphics.blit(MineChatTextures.PLAYER_FRAME, frameX, 158, 0, 0, 22, 22, 22, 22);

                        ResourceLocation head = playerCache.getSkinLocation();
                        guiGraphics.blit(head, headX, 161, 16, 16, 16, 16, 128, 128);
                        guiGraphics.blit(head, headX, 161, 80, 16, 16, 16, 128, 128);
                    } else {
                        //未知头像
                        guiGraphics.blit(MineChatTextures.UNKNOW, frameX, 158, 0, 0, 22, 22, 22, 22);
                    }
                } else if (sender.getSenderType() == SenderType.NPC && sender.getHead() != null) {
                    //头像背景框
                    guiGraphics.blit(MineChatTextures.PLAYER_FRAME, frameX, 158, 0, 0, 22, 22, 22, 22);
                    if (sender.isCustomHead()) {
                        guiGraphics.blit(sender.getHead(), headX, 161, 0, 0, 16, 16, 16, 16);
                    } else {
                        guiGraphics.blit(sender.getHead(), headX, 161, 16, 16, 16, 16, 128, 128);
                        guiGraphics.blit(sender.getHead(), headX, 161, 80, 16, 16, 16, 128, 128);
                    }
                } else {
                    //未知头像
                    guiGraphics.blit(MineChatTextures.UNKNOW, frameX, 158, 0, 0, 22, 22, 22, 22);
                }
            } else {
                guiGraphics.blit(MineChatTextures.SYSTEM_ICON, frameX, 158, 0, 0, 22, 22, 22, 22);
            }
        }


        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    protected void addRenderText(FormattedCharSequence text, int x, int y, int width) {
        renderedLines.add(new RenderedChatLine(text, x, y, width));
    }

    protected void addRenderText(Component text, int x, int y, int width) {
        renderedLines.add(new RenderedChatLine(text, x, y, width));
    }

    public void resetScroll() {
        this.scrollDelta = 0;
    }


    protected abstract @NotNull List<AnimationMessage> getChatMessages();


    private @Nullable ChatPicture getPictureAt(int mouseX, int mouseY) {
        for (RenderedChatPicture renderPicture : renderedChatPictures) {
            ChatPicture picture = renderPicture.picture;
            Vec2 size = picture.isSystem() ? picture.getDisplaySize(SYSTEM_LIMIT_WIDTH, SYSTEM_LIMIT_HEIGHT) : picture.getDisplaySize(LIMIT_WIDTH, LIMIT_HEIGHT);
            if (mouseX >= renderPicture.x && mouseX <= renderPicture.x + size.x && mouseY >= renderPicture.y && mouseY <= renderPicture.y + size.y) {
                return picture;
            }
        }
        return null;
    }

    @Nullable
    protected Style getStyleAt(int mouseX, int mouseY) {
        for (RenderedChatLine line : renderedLines) {
            if (mouseX >= line.x() && mouseX <= line.x() + line.width() && mouseY >= line.y() && mouseY <= line.y() + font.lineHeight) {
                return font.getSplitter().componentStyleAtWidth(line.text(), mouseX - line.x());
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //图片放大显示，屏蔽所有其他点击事件
        if (displayingPicture && !isPictureZoomingOut) {
            if (button == 0) {
                isMouseDown = true;
                isClick = true;
                isDragging = true;
                return true;
            }
            if (button == 2) {
                resetPictureZoom();
                return true;
            }
            return true;
        }

        if (button == 0) {
            if (messageBound != null && messageBound.inScissorBound((int) mouseX, (int) mouseY, 0, 0, showPicturePanel ? -messageBound.getHeight() * 3 / 4 : 0)) {
                ChatPicture chatPicture = getPictureAt((int) mouseX, (int) mouseY);
                if (chatPicture != null) {
                    RenderedChatPicture renderPicture = getRenderedPictureAt((int) mouseX, (int) mouseY);
                    if (renderPicture != null) {
                        Vec2 displaySize = chatPicture.getDisplaySize(chatPicture.isSystem() ? SYSTEM_LIMIT_WIDTH : LIMIT_WIDTH, chatPicture.isSystem() ? SYSTEM_LIMIT_HEIGHT : LIMIT_HEIGHT);
                        pictureStartX = renderPicture.x;
                        pictureStartY = renderPicture.y;
                        pictureStartWidth = displaySize.x;
                        pictureStartHeight = displaySize.y;
                    }
                    displayingPicture = true;
                    displayedPicture = chatPicture;
                    displayPictureTick = 0;
                    isPictureZoomingOut = false;

                    resetPictureZoom();
                    // 重置点击状态
                    isMouseDown = false;
                    isClick = false;
                    isDragging = false;
                    return true;
                }

                //打开了表情包菜单，屏蔽文本点击事件
                if(!showPicturePanel){
                    Style style = getStyleAt((int) mouseX, (int) mouseY);
                    if (style != null && this.handleComponentClicked(style)) {
                        return true;
                    }
                }
            }
        }

        if (button == 0 && isMouseOverMessageScrollBarThumb(mouseX, mouseY)) {
            draggingMessageScrollBar = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (displayingPicture && !isPictureZoomingOut) {
                isDragging = false;

                if (isMouseDown && isClick) {
                    isPictureZoomingOut = true;
                    isMouseDown = false;
                    isClick = false;
                    return true;
                }
                isMouseDown = false;
                isClick = false;
                return true;
            }
            isDragging = false;
            isMouseDown = false;
            isClick = false;
            draggingMessageScrollBar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (displayingPicture && !isPictureZoomingOut && isDragging && button == 0) {
            // 如果移动距离超过阈值，标记为拖动而非点击
            if (Math.abs(dragX) > 0 || Math.abs(dragY) > 0) {
                isClick = false;
            }

            pictureZoomOffsetX += (float) dragX;
            pictureZoomOffsetY += (float) dragY;

            clampPictureOffset();
            return true;
        }

        if (draggingMessageScrollBar && button == 0) {
            if (messageBound == null) {
                return true;
            }

            float contentHeight = totalLineHeight;
            float boundHeight = messageBound.totalYHeight();

            float maxScroll = Math.max(0, contentHeight - boundHeight);

            if (maxScroll <= 0 || boundHeight <= 0) {
                return true;
            }

            float thumbHeight = boundHeight * boundHeight / contentHeight;
            thumbHeight = Math.max(10, Math.min(boundHeight, thumbHeight));

            float maxThumbOffset = boundHeight - thumbHeight;

            if (maxThumbOffset <= 0) {
                return true;
            }

            // 鼠标移动 1 像素，对应多少内容滚动距离
            float scrollPerPixel = maxScroll / maxThumbOffset;

            this.scrollDelta = Mth.clamp(
                    this.scrollDelta - (float) dragY * scrollPerPixel,
                    0,
                    maxScroll
            );

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double delta) {
        // 如果正在预览图片，处理缩放
        if (displayingPicture) {
            // 获取鼠标在屏幕上的位置作为缩放中心
            float mouseX = (float) pMouseX;
            float mouseY = (float) pMouseY;

            // 计算缩放倍数（每次缩放 10%）
            float zoomDelta = (float) delta * 0.1f;
            float newScale = Mth.clamp(pictureZoomScale + zoomDelta, 0.1f, 10.0f);

            if (newScale != pictureZoomScale) {
                // 计算当前图片的显示位置
                Vec2 targetSize = displayedPicture.getDisplaySize(width, height);
                float targetX = (width - targetSize.x) / 2;
                float targetY = (height - targetSize.y) / 2;

                float currentWidth = targetSize.x;
                float currentHeight = targetSize.y;

                // 计算图片中心（加上当前偏移）
                float centerX = targetX + currentWidth / 2 + pictureZoomOffsetX;
                float centerY = targetY + currentHeight / 2 + pictureZoomOffsetY;

                // 计算鼠标相对于图片中心的位置（归一化坐标，考虑当前缩放）
                float relX = (mouseX - centerX) / (currentWidth * pictureZoomScale);
                float relY = (mouseY - centerY) / (currentHeight * pictureZoomScale);

                // 更新缩放
                pictureZoomScale = newScale;

                // 重新计算偏移，使鼠标位置保持不变
                pictureZoomOffsetX = (mouseX - targetX - currentWidth / 2) - relX * currentWidth * pictureZoomScale;
                pictureZoomOffsetY = (mouseY - targetY - currentHeight / 2) - relY * currentHeight * pictureZoomScale;

                // 限制偏移量
                clampPictureOffset();
            }

            return true;
        }

        // 否则处理聊天滚动
        if (messageBound != null && messageBound.inScissorBound((int) pMouseX, (int) pMouseY, 0, 0)) {
            if (!hasShiftDown()) {
                delta *= 20;
            }

            if (totalLineHeight > messageBound.totalYHeight()) {
                this.scrollDelta = (float) Mth.clamp(this.scrollDelta + delta, 0, this.totalLineHeight - messageBound.totalYHeight());
            }
            return true;
        }
        return false;
    }


    protected void resetPictureZoom() {
        pictureZoomScale = 1.0f;
        pictureZoomTargetScale = 1.0f;
        pictureZoomOffsetX = 0f;
        pictureZoomOffsetY = 0f;
        isDragging = false;
        isMouseDown = false;
        isClick = false;
    }


    @Override
    public void onClose() {
        super.onClose();
        stopAnimation();
        displayingPicture = false;
        displayedPicture = null;
        displayPictureTick = 0;
        isPictureZoomingOut = false;
        resetPictureZoom();
    }

    protected void clampPictureOffset() {
        if (displayedPicture == null) return;

        Vec2 targetSize = displayedPicture.getDisplaySize(width, height);
        float targetX = (width - targetSize.x) / 2;
        float targetY = (height - targetSize.y) / 2;

        float scaledWidth = targetSize.x * pictureZoomScale;
        float scaledHeight = targetSize.y * pictureZoomScale;

        float centerX = targetX + targetSize.x / 2 + pictureZoomOffsetX;
        float centerY = targetY + targetSize.y / 2 + pictureZoomOffsetY;

        float left = centerX - scaledWidth / 2;
        float top = centerY - scaledHeight / 2;
        float right = centerX + scaledWidth / 2;
        float bottom = centerY + scaledHeight / 2;

        float minVisibleRatio = 0.1f;

        if (scaledWidth > width) {
            if (left > width * minVisibleRatio) {
                float offsetDelta = left - width * minVisibleRatio;
                pictureZoomOffsetX -= offsetDelta;
            } else if (right < width * (1 - minVisibleRatio)) {
                float offsetDelta = width * (1 - minVisibleRatio) - right;
                pictureZoomOffsetX += offsetDelta;
            }
        } else {
            float maxOffset = width * 0.8f;
            pictureZoomOffsetX = Mth.clamp(pictureZoomOffsetX, -maxOffset, maxOffset);
        }

        if (scaledHeight > height) {
            if (top > height * minVisibleRatio) {
                float offsetDelta = top - height * minVisibleRatio;
                pictureZoomOffsetY -= offsetDelta;
            } else if (bottom < height * (1 - minVisibleRatio)) {
                float offsetDelta = height * (1 - minVisibleRatio) - bottom;
                pictureZoomOffsetY += offsetDelta;
            }
        } else {
            float maxOffset = height * 0.8f;
            pictureZoomOffsetY = Mth.clamp(pictureZoomOffsetY, -maxOffset, maxOffset);
        }
    }

    private @Nullable RenderedChatPicture getRenderedPictureAt(int mouseX, int mouseY) {
        for (RenderedChatPicture renderPicture : renderedChatPictures) {
            ChatPicture picture = renderPicture.picture;
            Vec2 size = picture.isSystem() ? picture.getDisplaySize(SYSTEM_LIMIT_WIDTH, SYSTEM_LIMIT_HEIGHT) : picture.getDisplaySize(LIMIT_WIDTH, LIMIT_HEIGHT);
            if (mouseX >= renderPicture.x && mouseX <= renderPicture.x + size.x && mouseY >= renderPicture.y && mouseY <= renderPicture.y + size.y) {
                return renderPicture;
            }
        }
        return null;
    }

    @Override
    protected void insertText(@NotNull String pText, boolean pOverwrite) {
        //根据情况跳转至私聊或者其他界面
        Minecraft mc = Minecraft.getInstance();
        if (mainEditBox != null) {
            if (pText.startsWith("/tell")) {
                String[] text = pText.split(" ");
                mc.setScreen(new MineChatDMScreen(text[text.length - 1]));
            } else if (pText.startsWith("/")) {
                mc.setScreen(new ChatScreen(pText));
                stopAnimation();
            } else {
                if (pOverwrite) {
                    this.mainEditBox.setValue(pText);
                } else {
                    this.mainEditBox.insertText(pText);
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 优先处理输入框的按键事件
        if (mainEditBox != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                LocalPlayer player = getMinecraft().player;
                if (player != null) {
                    onEditBoxEnterPressed(player);
                }
                historyPos = -1;
                historyBuffer = "";
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_UP) {
                moveInHistory(-1);
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                moveInHistory(1);
                return true;
            }
        }

        // 其他按键交给父类处理
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected boolean handleMentionTabCompletion() {
        if (mainEditBox == null) return false;

        String text = mainEditBox.getValue();
        int cursorPos = mainEditBox.getCursorPosition();

        // 查找当前输入的 @ 位置
        int atIndex = text.lastIndexOf('@', cursorPos);
        if (atIndex == -1) return false;

        // 检查 @ 后面是否已经有内容（查找空格或结尾）
        int spaceIndex = text.indexOf(' ', atIndex);
        if (spaceIndex == -1) spaceIndex = text.length();

        // 获取当前输入的玩家名称前缀
        String prefix = text.substring(atIndex + 1, Math.min(cursorPos, spaceIndex));

        // 如果是第一次按 Tab，获取所有匹配的玩家
        if (!isMentionCompleting) {
            // 获取所有在线玩家
            List<String> players = getOnlinePlayerNames();
            mentionSuggestions.clear();
            mentionSuggestionIndex = 0;

            // 过滤匹配前缀的玩家
            for (String name : players) {
                if (name.toLowerCase().startsWith(prefix.toLowerCase())) {
                    mentionSuggestions.add(name);
                }
            }

            // 按字母排序
            Collections.sort(mentionSuggestions);

            if (mentionSuggestions.isEmpty()) {
                return false;
            }

            isMentionCompleting = true;
        }

        // 获取当前建议的玩家名
        if (mentionSuggestions.isEmpty()) {
            isMentionCompleting = false;
            return false;
        }

        String suggestion = mentionSuggestions.get(mentionSuggestionIndex % mentionSuggestions.size());
        mentionSuggestionIndex++;

        // 构建新文本：@玩家名 + 空格
        String newText = text.substring(0, atIndex) + "@" + suggestion + " ";
        if (cursorPos < text.length()) {
            newText += text.substring(cursorPos);
        }

        mainEditBox.setValue(newText);
        mainEditBox.setCursorPosition(atIndex + suggestion.length() + 2); // @ + name + space

        return true;
    }

    protected List<String> getOnlinePlayerNames() {
        List<String> names = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                String name = info.getProfile().getName();
                if (name != null) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    public void moveInHistory(int msgPos) {
        if (mainEditBox != null) {
            List<String> history = getMinecraft().gui.getChat().getRecentChat();
            int newPos = this.historyPos + msgPos;
            int max = history.size();
            if (newPos < 0) {
                newPos = max - 1;
            }
            newPos = Mth.clamp(newPos, 0, max - 1);
            if (newPos == this.historyPos) return;

            if (newPos == max) {
                this.historyPos = max;
                this.mainEditBox.setValue(this.historyBuffer);
            } else {
                if (this.historyPos == max) {
                    this.historyBuffer = this.mainEditBox.getValue();
                }

                this.mainEditBox.setValue(history.get(newPos));
                this.mainEditBox.setCursorPosition(this.mainEditBox.getValue().length());
                this.historyPos = newPos;
            }
            mainEditBox.setFocused(true);
        }
    }

    protected void onEditBoxEnterPressed(LocalPlayer player) {
        getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 1.0F));
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public enum CurrentPage {
        GLOBE, DM, TEAM
    }

    public record AnimationParam(int timestamp, int posOffsetX, int posOffsetY, float transition) {
    }

    public record RenderedChatLine(FormattedCharSequence text, int x, int y, int width) {
        RenderedChatLine(Component text, int x, int y, int width) {
            this(text.getVisualOrderText(), x, y, width);
        }
    }

    public record RenderedChatPicture(ChatPicture picture, int x, int y) {

    }

    public static class ScissorBound {
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
            return ((posX + width) >= startX && posX <= endX) && ((posY + height) >= startY && posY <= endY);
        }

        public boolean inScissorBound(int posX, int posY, int width, int height, int heightOffset) {
            return ((posX + width) >= startX && posX <= endX) && ((posY + height) >= startY && posY <= endY + heightOffset);
        }

        public int totalXHeight() {
            return endX - startX;
        }

        public int totalYHeight() {
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

        public int getWidth() {
            return endX - startX;
        }

        public int getHeight() {
            return endY - startY;
        }
    }

    public record MessageRenderInfo(AnimationMessage message, int x, int y, int width, int height) {}
}
