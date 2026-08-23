package net.shelmarow.mine_chat.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.hud.MineChatHudRenderer;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = MineChat.MOD_ID, value = Dist.CLIENT)
public class ModClientEvent {

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("chat_hud", MineChatHudRenderer.instance);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.START){
            ClientPictureManager.getInstance().tickCustomPictureLoading();
        }
    }

    @SubscribeEvent
    public static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected @NotNull Void prepare(@NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
                return null;
            }
            @Override
            protected void apply(@NotNull Void object, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
                ClientPictureManager.getInstance().load(manager);
            }
        });
    }
}
