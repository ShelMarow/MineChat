package net.shelmarow.mine_chat.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.hud.MineChatHudRenderer;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = MineChat.MOD_ID, value = Dist.CLIENT)
public class ModClientEvent {

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "chat_hud"), MineChatHudRenderer.instance);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientPictureManager.getInstance().tickCustomPictureLoading();
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
