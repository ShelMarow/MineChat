package net.shelmarow.mine_chat.chat.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.message.AnimationMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MineChatTeamScreen extends MineChatScreen {

    private boolean haveTeam = false;

    public MineChatTeamScreen() {
        super();
        this.currentPage = CurrentPage.TEAM;
        this.nameLeftOffsetX = -4;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            haveTeam = mc.player.getTeam() != null;
        }
    }

    @Override
    public @NotNull List<AnimationMessage> getChatMessages() {
        if(haveTeam) {
            List<AnimationMessage> messages = MineChatManager.getTeamMessages();
            Collections.reverse(messages);
            MineChatManager.checkTeam();
            return messages;
        }
        else {
            MineChatManager.clearTeamMessage();
            return Collections.emptyList();
        }
    }
    @Override
    protected void renderAfterBackground(@NotNull GuiGraphics guiGraphics, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null) return;
        if(!haveTeam){

            guiGraphics.drawCenteredString(font, Component.translatable("text.mine_chat.not_in_team"), bgWidth / 2, bgHeight / 2, 0xFFFFFF);
        }
        else{
            Team team = mc.player.getTeam();
            if(team != null) {
                PlayerTeam playerTeam = mc.player.getScoreboard().getPlayerTeam(team.getName());
                if(playerTeam != null) {
                    guiGraphics.drawString(font,  playerTeam.getFormattedDisplayName(), 8 + 3 * (52 + 4), 11 , 0xFFFFFF);
                }
            }
        }
    }

    @Override
    public @NotNull MineChatScreen.SenderWithMessage getDisplayMessage(AnimationMessage message) {
        return super.getDisplayMessage(message);
    }

    @Override
    protected void onEditBoxEnterPressed(LocalPlayer player) {
        super.onEditBoxEnterPressed(player);
        if(currentPage == CurrentPage.TEAM) {
            String message = this.mainEditBox.getValue();
            if(haveTeam && !message.isEmpty()) {
                player.connection.sendCommand("teammsg " + message);
                resetScroll();
            }
            this.mainEditBox.setValue("");
        }
    }

}
