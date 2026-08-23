package net.shelmarow.mine_chat.chat.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import net.shelmarow.mine_chat.chat.screen.button.MButton;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@OnlyIn(Dist.CLIENT)
public class CustomPictureConfigScreen extends Screen {

    private static final int PATH_ENTRY_HEIGHT = 30;
    private static final int DELETE_BUTTON_WIDTH = 50;
    private static final int DELETE_BUTTON_HEIGHT = 20;
    private static final int MAX_SCAN_FILES = 1000;

    private final Screen parent;
    private final List<Path> paths = new ArrayList<>();
    private final List<MButton> deleteButtons = new ArrayList<>();

    private int scrollOffset;

    private int listLeft;
    private int listTop;
    private int listWidth;
    private int listHeight;

    private MButton addButton;

    private boolean draggingScrollbar;
    private double scrollbarDragOffset;

    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_MARGIN = 2;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 20;

    private boolean showInfo;
    private boolean messageIsError;
    private Component infoMessage;

    public CustomPictureConfigScreen(Screen parent) {
        super(Component.translatable("mine_chat.picture.custom_config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        clearWidgets();
        deleteButtons.clear();

        listWidth = Math.min(600, width - 40);
        listHeight = height - 80;
        listLeft = (width - listWidth) / 2;
        listTop = 35;

        loadPaths();

        addButton = new MButton(width / 2 - 135, height - 30, 90, 20,
                Component.translatable("mine_chat.picture.custom_config.add_file"),
                button -> openCustomPictureSelector());
        addRenderableWidget(addButton);

        MButton closeButton = new MButton(width / 2 + 45, height - 30, 90, 20,
                Component.translatable("mine_chat.picture.custom_config.close"),
                button -> onClose());
        addRenderableWidget(closeButton);

        createDeleteButtons();
    }

    private void loadPaths() {
        paths.clear();

        ClientPictureManager manager = ClientPictureManager.getInstance();

        paths.addAll(manager.getCustomPicturePaths());

        paths.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));

        scrollOffset = clampScrollOffset();
    }

    private void createDeleteButtons() {
        for (MButton button : deleteButtons) {
            removeWidget(button);
        }

        deleteButtons.clear();

        for (int i = 0; i < paths.size(); i++) {
            MButton button = new MButton(0, 0, DELETE_BUTTON_WIDTH, DELETE_BUTTON_HEIGHT,
                    Component.translatable("mine_chat.picture.custom_config.delete"),
                    this::handleDeleteButton
            );

            button.visible = false;

            deleteButtons.add(button);
            addRenderableWidget(button);
        }

        updateDeleteButtons();
    }

    private void handleDeleteButton(MButton button) {
        if (showInfo) {
            return;
        }

        int index = deleteButtons.indexOf(button);
        if (index < 0 || index >= paths.size()) {
            return;
        }

        removePath(paths.get(index));
        //saveAndLoad();
    }

    private void updateDeleteButtons() {
        for (int i = 0; i < deleteButtons.size(); i++) {
            MButton button = deleteButtons.get(i);

            if (i >= paths.size()) {
                button.visible = false;
                continue;
            }

            int y = listTop + i * PATH_ENTRY_HEIGHT - scrollOffset + 5;
            boolean visible = y + DELETE_BUTTON_HEIGHT > listTop && y < listTop + listHeight;

            button.visible = visible;
            if (visible) {
                button.setX(listLeft + listWidth - DELETE_BUTTON_WIDTH - 10);
                button.setY(y);
            }
        }
    }

    private int getMaxScrollOffset() {
        int contentHeight = paths.size() * PATH_ENTRY_HEIGHT;
        return Math.max(0, contentHeight - listHeight);
    }

    private int clampScrollOffset() {
        return Math.max(0, Math.min(scrollOffset, getMaxScrollOffset()));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        guiGraphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);

        guiGraphics.fill(listLeft, listTop, listLeft + listWidth, listTop + listHeight, 0x60101010);
        guiGraphics.renderOutline(listLeft, listTop, listWidth, listHeight, 0xFFFFFFFF);

        guiGraphics.enableScissor(listLeft, listTop + 1, listLeft + listWidth, listTop + listHeight - 1);

        renderPaths(guiGraphics, mouseX, mouseY);

        updateDeleteButtons();

        for (MButton mButton : deleteButtons) {
            if (mButton.visible) {
                mButton.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        guiGraphics.disableScissor();


        renderScrollbar(guiGraphics, mouseX, mouseY);

        for (Renderable renderable : this.renderables) {
            if(renderable instanceof MButton mButton && deleteButtons.contains(mButton)) {
                continue;
            }
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        if (showInfo) {
            PoseStack poseStack = guiGraphics.pose();

            poseStack.pushPose();
            poseStack.translate(0,0,1);

            renderInfoWindow(guiGraphics, mouseX, mouseY);

            poseStack.popPose();
        }
    }

    private void renderPaths(GuiGraphics guiGraphics, int mouseX, int mouseY) {

        if (paths.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("mine_chat.picture.custom_config.empty"),
                    listLeft + listWidth / 2, listTop + listHeight / 2 - 5, 0xAAAAAA);

            return;
        }

        int startIndex = scrollOffset / PATH_ENTRY_HEIGHT;

        int endIndex = Math.min(paths.size(), startIndex + listHeight / PATH_ENTRY_HEIGHT + 2);

        for (int i = startIndex; i < endIndex; i++) {
            Path path = paths.get(i);

            int entryY = listTop + i * PATH_ENTRY_HEIGHT - scrollOffset;

            boolean inBound = entryY + PATH_ENTRY_HEIGHT > listTop && entryY < listTop + listHeight;

            if(inBound) {
                renderPathEntry(guiGraphics, path, entryY, mouseX, mouseY);
            }
        }
    }

    private void renderPathEntry(GuiGraphics guiGraphics, Path path, int y, int mouseX, int mouseY) {
        int textX = listLeft + 6;
        int textY = y + (PATH_ENTRY_HEIGHT) / 2 - 4;

        int deleteX = listLeft + listWidth - DELETE_BUTTON_WIDTH - 10;

        boolean hovering = mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= y && mouseY < y + PATH_ENTRY_HEIGHT;

        guiGraphics.fill(listLeft + 2, y + 1, listLeft + listWidth - 2, y + PATH_ENTRY_HEIGHT - 1, hovering ? 0x60606060 : 0x20202020);

        String text = path.toString();

        int textWidth = deleteX - textX - 8;

        String displayText = font.plainSubstrByWidth(text, textWidth);

        if (!displayText.equals(text)) {
            displayText += "...";
        }

        boolean exists = Files.exists(path);

        guiGraphics.drawString(font, displayText, textX, textY, exists ? 0xFFFFFF : 0xFF5555, false);

        if (!exists) {
            guiGraphics.drawString(font, Component.translatable("mine_chat.picture.custom_config.invalid"), textX, textY + 10, 0xFF5555, false);
        }
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (getMaxScrollOffset() <= 0) {
            return;
        }

        int scrollbarX = getScrollbarX();
        int scrollbarTop = getScrollbarTop();
        int scrollbarHeight = getScrollbarHeight();

        int thumbHeight = getScrollbarThumbHeight();
        int thumbY = getScrollbarThumbY();

        // 滚动条背景
        guiGraphics.fill(scrollbarX, scrollbarTop, scrollbarX + SCROLLBAR_WIDTH, scrollbarTop + scrollbarHeight, 0x60303030);

        boolean hovering = mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;

        int thumbColor;

        if (draggingScrollbar) {
            thumbColor = 0xFFFFFFFF;
        } else if (hovering) {
            thumbColor = 0xFFCCCCCC;
        } else {
            thumbColor = 0xFF999999;
        }

        guiGraphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, thumbColor);
    }


    @Override
    public void onFilesDrop(@NotNull List<Path> paths) {
        showInfoWindow(Component.translatable("mine_chat.picture.loading"), false);
        ClientPictureManager.getInstance().loadDropFiles(paths);
        loadPaths();
        createDeleteButtons();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showInfo) {
            return true;
        }

        if (isInsideList(mouseX, mouseY)) {
            scrollOffset -= (int) (delta * PATH_ENTRY_HEIGHT * 0.25);

            scrollOffset = clampScrollOffset();

            updateDeleteButtons();

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showInfo) {
            if (button == 0 && isInfoConfirmButton(mouseX, mouseY)) {
                closeInfo();
            }

            return true;
        }

        if (button == 0 && isMouseOverScrollbarThumb(mouseX, mouseY)) {
            draggingScrollbar = true;
            scrollbarDragOffset = mouseY - getScrollbarThumbY();

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            scrollbarDragOffset = 0.0;

            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (showInfo) {
            return true;
        }

        if (draggingScrollbar && button == 0) {
            updateScrollFromMouseY(mouseY);

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isInsideList(double mouseX, double mouseY) {
        return mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= listTop && mouseY <= listTop + listHeight;
    }

    private void updateScrollFromMouseY(double mouseY) {
        int maxScroll = getMaxScrollOffset();

        if (maxScroll <= 0) {
            scrollOffset = 0;
            return;
        }

        int scrollbarTop = getScrollbarTop();
        int scrollbarHeight = getScrollbarHeight();
        int thumbHeight = getScrollbarThumbHeight();

        int maxThumbOffset = scrollbarHeight - thumbHeight;

        if (maxThumbOffset <= 0) {
            scrollOffset = 0;
            return;
        }

        double thumbY = mouseY - scrollbarDragOffset;
        double thumbOffset = thumbY - scrollbarTop;

        thumbOffset = Math.max(0.0, Math.min(thumbOffset, maxThumbOffset));
        double progress = thumbOffset / maxThumbOffset;

        scrollOffset = (int) Math.round(progress * maxScroll);
        scrollOffset = clampScrollOffset();

        updateDeleteButtons();
    }

    private boolean isMouseOverScrollbarThumb(double mouseX, double mouseY) {
        if (getMaxScrollOffset() <= 0) {
            return false;
        }

        int scrollbarX = getScrollbarX();
        int thumbY = getScrollbarThumbY();
        int thumbHeight = getScrollbarThumbHeight();

        return mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
    }

    private int getScrollbarX() {
        return listLeft + listWidth - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
    }

    private int getScrollbarTop() {
        return listTop + SCROLLBAR_MARGIN;
    }

    private int getScrollbarHeight() {
        return listHeight - SCROLLBAR_MARGIN * 2;
    }


    private int getScrollbarThumbHeight() {
        int contentHeight = paths.size() * PATH_ENTRY_HEIGHT;

        if (contentHeight <= listHeight) {
            return 0;
        }

        int scrollbarHeight = getScrollbarHeight();

        return Math.max(
                SCROLLBAR_MIN_THUMB_HEIGHT,
                scrollbarHeight * listHeight / contentHeight
        );
    }

    private int getScrollbarThumbY() {
        int maxScroll = getMaxScrollOffset();

        if (maxScroll <= 0) {
            return getScrollbarTop();
        }

        int scrollbarHeight = getScrollbarHeight();
        int thumbHeight = getScrollbarThumbHeight();

        int maxThumbOffset = scrollbarHeight - thumbHeight;

        if (maxThumbOffset <= 0) {
            return getScrollbarTop();
        }

        return getScrollbarTop()
                + (int) Math.round(
                (double) maxThumbOffset * scrollOffset / maxScroll
        );
    }




    protected void openCustomPictureSelector() {
        Minecraft mc = Minecraft.getInstance();

        if (addButton != null) {
            addButton.active = false;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                String defaultPath = mc.gameDirectory.getAbsolutePath();

                String selected = TinyFileDialogs.tinyfd_selectFolderDialog("选择自定义表情文件夹", defaultPath);

                if (selected == null || selected.isBlank()) {
                    return FolderSelection.cancelled();
                }

                Path path = Path.of(selected).toAbsolutePath().normalize();

                if (!Files.exists(path)) {
                    MineChat.LOGGER.warn("[MineChat] Selected folder does not exist: {}", path);

                    return FolderSelection.error(Component.translatable("mine_chat.picture.custom_config.folder_not_exist"));
                }

                if (!Files.isDirectory(path)) {
                    MineChat.LOGGER.warn("[MineChat] Selected path is not a directory: {}", path);

                    return FolderSelection.error(Component.translatable("mine_chat.picture.custom_config.not_directory"));
                }

                if (!Files.isReadable(path)) {
                    MineChat.LOGGER.warn("[MineChat] Selected folder is not readable: {}", path);

                    return FolderSelection.error(Component.translatable("mine_chat.picture.custom_config.not_readable"));
                }

                if (path.getParent() == null) {
                    MineChat.LOGGER.warn("[MineChat] Refused filesystem root: {}", path);

                    return FolderSelection.error(Component.translatable("mine_chat.picture.custom_config.root_forbidden"));
                }

                int fileCount = 0;

                try (var stream = Files.walk(path)) {

                    var iterator = stream.iterator();

                    while (iterator.hasNext()) {
                        Path current = iterator.next();

                        if (!Files.isRegularFile(current)) {
                            continue;
                        }

                        fileCount++;

                        if (fileCount > MAX_SCAN_FILES) {

                            MineChat.LOGGER.warn("[MineChat] Selected folder contains more than {} files: {}", MAX_SCAN_FILES, path);

                            return FolderSelection.error(Component.translatable("mine_chat.picture.custom_config.too_many_files", MAX_SCAN_FILES));
                        }
                    }
                }

                return FolderSelection.success(path);

            } catch (Exception e) {
                MineChat.LOGGER.error("[MineChat] Failed to select custom picture folder", e);

                return FolderSelection.error(Component.translatable("mine_chat.picture.custom_config.selection_error"));
            }
        }).thenAccept(result -> mc.execute(() -> {
            if (addButton != null) {
                addButton.active = true;
            }

            if (result == null) {
                return;
            }

            if (result.error() != null) {
                showInfoWindow(result.error(), true);
                return;
            }

            if (!result.isSuccess()) {
                return;
            }

            Path path = result.path();

            ClientPictureManager manager = ClientPictureManager.getInstance();

            showInfoWindow(Component.translatable("mine_chat.picture.loading"), false);
            boolean added = manager.addCustomPicturePath(path);

            if (added) {
                loadPaths();
                createDeleteButtons();
            } else {
                showInfoWindow(Component.translatable("mine_chat.picture.custom_config.add_failed"), true);
            }
        }));
    }

    private void removePath(Path path) {
        ClientPictureManager manager = ClientPictureManager.getInstance();

        if (!manager.removeCustomPicturePath(path)) {
            return;
        }

        loadPaths();
        createDeleteButtons();
    }


    private void showInfoWindow(Component message, boolean isError) {
        infoMessage = message;
        showInfo = true;
        messageIsError  = isError;
        draggingScrollbar = false;
    }

    private void closeInfo() {
        showInfo = false;
        infoMessage = null;
    }

    private void renderInfoWindow(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int windowWidth = 320;
        int windowHeight = 120;

        int x = (width - windowWidth) / 2;

        int y = (height - windowHeight) / 2;

        guiGraphics.fill(0, 0, width, height, 0x88000000);

        guiGraphics.fill(x, y, x + windowWidth, y + windowHeight, 0xFF202020);

        guiGraphics.renderOutline(x, y, windowWidth, windowHeight, 0xFFFFFFFF);

        guiGraphics.drawCenteredString(font,
                messageIsError ?
                        Component.translatable("mine_chat.picture.custom_config.error")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD, ChatFormatting.UNDERLINE) :
                        Component.translatable("mine_chat.picture.custom_config.info")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD, ChatFormatting.UNDERLINE),
                width / 2, y + 12, 0xFFFFFF);

        if (infoMessage != null) {
            int maxWidth = windowWidth - 30;

            List<FormattedCharSequence> lines = font.split(infoMessage, maxWidth);

            int textY = y + 35;

            for (FormattedCharSequence line : lines) {
                guiGraphics.drawCenteredString(font, line, width / 2, textY, 0xFFFFFF);

                textY += font.lineHeight;
            }
        }

        int buttonWidth = 70;
        int buttonHeight = 20;

        int buttonX = x + (windowWidth - buttonWidth) / 2;

        int buttonY = y + windowHeight - 30;

        boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;

        guiGraphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, hovered ? 0xFF505050 : 0xFF303030);

        guiGraphics.renderOutline(buttonX, buttonY, buttonWidth, buttonHeight, hovered ? 0xFFFFFF00 : 0xFFFFFFFF);

        guiGraphics.drawCenteredString(font, Component.translatable("gui.ok"), buttonX + buttonWidth / 2, buttonY + 6, 0xFFFFFF);
    }

    private boolean isInfoConfirmButton(double mouseX, double mouseY) {
        int windowWidth = 320;
        int windowHeight = 120;

        int x = (width - windowWidth) / 2;

        int y = (height - windowHeight) / 2;

        int buttonWidth = 70;
        int buttonHeight = 20;

        int buttonX = x + (windowWidth - buttonWidth) / 2;

        int buttonY = y + windowHeight - 30;

        return mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
    }

    @Override
    public void onClose() {
        if (parent != null) {
            Minecraft.getInstance().setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record FolderSelection(Path path, Component error) {

        private static FolderSelection success(Path path) {
            return new FolderSelection(path, null);
        }

        private static FolderSelection cancelled() {
            return new FolderSelection(null, null);
        }

        private static FolderSelection error(Component message) {
            return new FolderSelection(null, message);
        }

        private boolean isSuccess() {
            return path != null;
        }
    }
}