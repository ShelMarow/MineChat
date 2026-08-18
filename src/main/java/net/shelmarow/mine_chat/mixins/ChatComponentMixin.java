package net.shelmarow.mine_chat.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow
    public abstract boolean isChatFocused();

    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private void onRender(GuiGraphics guiGraphics, int tickCount, int mouseX, int mouseY, boolean focused, CallbackInfo ci){
        if (!isChatFocused()){
            ci.cancel();
        }
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("TAIL")
    )
    private void onAddMessage(Component chatComponent, MessageSignature headerSignature, GuiMessageTag tag, CallbackInfo ci, @Local GuiMessage guimessage){
//
//        Minecraft minecraft = Minecraft.getInstance();
//        long timestamp = 0;
//        if(minecraft.level != null){
//            timestamp = minecraft.level.getGameTime();
//        }
//
//        boolean isInGlobeScreen = minecraft.screen instanceof MineChatGlobeScreen;
//
//        Component msg = guimessage.content();
//        msg = Component.empty()
//                .append(Component.literal("<").withStyle(ChatFormatting.YELLOW))
//                .append(Component.translatable("text.mine_chat.system").withStyle(ChatFormatting.YELLOW))
//                .append(Component.literal("> ").withStyle(ChatFormatting.YELLOW))
//                .append(msg);
//
//        MineChatManager.TeamNameInfo nameInfo = MineChatManager.getNameLength(Util.NIL_UUID, MessageType.SYSTEM);
//
//        ChatSender chatSender = new ChatSender(Util.NIL_UUID, "", null, SenderType.SYSTEM);
//
//        //加入全局历史消息
//        MineChatManager.addAnimationMessageToList(MineChatManager.getChatGlobe() ,
//                new AnimationMessage(chatSender, timestamp, nameInfo.getTotalLength(), MessageType.SYSTEM, isInGlobeScreen ? 5 : 0 , 0, 0, msg));
//
//        //加入全局最新消息显示队列
//        MineChatManager.addAnimationMessageToList(MineChatManager.getChatGlobeDisplay() ,
//                new AnimationMessage(chatSender, timestamp, nameInfo.getTotalLength(), MessageType.SYSTEM, msg), MineChatConfig.MAX_DISPLAYED_MESSAGES.getAsInt());
    }
}
