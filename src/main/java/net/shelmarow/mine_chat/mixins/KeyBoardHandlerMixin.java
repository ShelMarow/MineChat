package net.shelmarow.mine_chat.mixins;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.message.AnimationMessage;
import net.shelmarow.mine_chat.chat.message.chat_enum.MessageType;
import net.shelmarow.mine_chat.chat.screen.MineChatGlobeScreen;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.SenderType;
import net.shelmarow.mine_chat.config.MineChatClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.util.function.Consumer;

@Mixin(KeyboardHandler.class)
public class KeyBoardHandlerMixin {

    @Redirect(
            method = "keyPress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Screenshot;grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V"
            )
    )
    public void keyPressed(File gameDirectory, RenderTarget buffer, Consumer<Component> messageConsumer) {

        Screenshot.grab(gameDirectory, buffer, component->{
            Minecraft minecraft = Minecraft.getInstance();

            minecraft.gui.getChat().addMessage(component);

            long timestamp = 0;
            if(minecraft.level != null){
                timestamp = minecraft.level.getGameTime();
            }

            boolean isInGlobeScreen = minecraft.screen instanceof MineChatGlobeScreen;

            Component msg = component;
            msg = Component.empty()
                    .append(Component.literal("<").withStyle(ChatFormatting.YELLOW))
                    .append(Component.translatable("text.mine_chat.system").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("> ").withStyle(ChatFormatting.YELLOW))
                    .append(msg);

            MineChatManager.TeamNameInfo nameInfo = MineChatManager.getNameLength(Util.NIL_UUID, MessageType.SYSTEM);

            ChatSender chatSender = new ChatSender(Util.NIL_UUID, "", null, SenderType.SYSTEM);

            //加入全局历史消息
            MineChatManager.addAnimationMessageToList(MineChatManager.getChatGlobe() ,
                    new AnimationMessage(chatSender, timestamp, nameInfo.getTotalLength(), MessageType.SYSTEM, isInGlobeScreen ? 5 : 0 , 0, 0, msg));

            //加入全局最新消息显示队列
            MineChatManager.addAnimationMessageToList(MineChatManager.getChatGlobeDisplay() ,
                    new AnimationMessage(chatSender, timestamp, nameInfo.getTotalLength(), MessageType.SYSTEM, msg), MineChatClientConfig.MAX_DISPLAYED_MESSAGES.getAsInt());

        });
}
}
