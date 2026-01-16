package net.shelmarow.mine_chat.mixins;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow protected abstract boolean isChatFocused();

    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private void onRender(GuiGraphics pGuiGraphics, int pTickCount, int pMouseX, int pMouseY, CallbackInfo ci){
        if (!isChatFocused()){
            ci.cancel();
        }
    }
}
