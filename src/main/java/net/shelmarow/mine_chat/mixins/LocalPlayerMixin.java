package net.shelmarow.mine_chat.mixins;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.message.chat_enum.MessageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "sendSystemMessage", at = @At("TAIL"))
    private void sendSystemMessage(Component component, CallbackInfo ci) {
        Component message = Component.literal("<").append(Component.translatable("text.mine_chat.system").withStyle(ChatFormatting.YELLOW)).append(">").append(component);
        MineChatManager.TeamNameInfo nameLength = MineChatManager.getNameLength(Util.NIL_UUID, MessageType.SYSTEM);
        MineChatManager.addGlobeMessage(Util.NIL_UUID, MessageType.SYSTEM, message, true, nameLength.getTotalLength());
    }
}
