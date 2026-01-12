package net.shelmarow.mine_chat.chat.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.MineChatTextures;

import java.util.ArrayList;
import java.util.List;

public class MineChatHudRenderer implements IGuiOverlay {
    public static final MineChatHudRenderer instance = new MineChatHudRenderer();
    private static final Minecraft MC = Minecraft.getInstance();
    private static final List<Component> LATEST = new ArrayList<>();

    @Override
    public void render(ForgeGui forgeGui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        int x = 4;
        int y = screenHeight / 3;

        PoseStack poseStack = guiGraphics.pose();

        guiGraphics.blit(MineChatTextures.CHAT_ICON, x, y + 16, 0, 0, 16, 16, 16, 16);

        long time = System.currentTimeMillis() % 3000;
        float ratio = 1F;
        if(time <= 250){
            ratio = time / 250F;
        }
        else if(time > 1750 && time <= 2000){
            ratio = 1 - ((time - 1750) / 250F);
        }
        else if(time > 2000){
            ratio = 0;
        }
        poseStack.pushPose();
        poseStack.translate(x, y + 32,0);
        poseStack.scale(ratio,ratio,1);

        guiGraphics.blit(MineChatTextures.CHAT_RECENT_MESSAGE, 0, 0, 0, 0, 128, 35, 128, 35);

        Component messages = MineChatManager.getLatestMessage();
        if(messages != null) {
            Font font = forgeGui.getFont();
            List<FormattedCharSequence> lines = ComponentRenderUtils.wrapComponents(messages, 120 - font.width("..."), font);
            for (int i = 0; i < lines.size(); i++) {
                FormattedCharSequence line = lines.get(i);
                guiGraphics.drawString(font, line, 2, 5 + i * font.lineHeight, 0xFFFFFF);
                if(i == 2 && lines.size() > 3){
                    guiGraphics.drawString(font, "...", 2 + font.width(line), 5 + i * font.lineHeight, 0xFFFFFF);
                    break;
                }
            }
        }

        poseStack.popPose();

    }
}
