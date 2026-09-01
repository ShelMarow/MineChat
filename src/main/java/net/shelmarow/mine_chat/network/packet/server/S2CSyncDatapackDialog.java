package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.mine_chat.chat.datapack.DialogReloadListener;
import net.shelmarow.mine_chat.chat.npc.NPCDialog;
import net.shelmarow.mine_chat.chat.npc.NPCDialogRegister;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.NPCSenderManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CSyncDatapackDialog {

    private final List<DialogReloadListener.NPCDialogProvider> providers;

    public S2CSyncDatapackDialog(List<DialogReloadListener.NPCDialogProvider> providers) {
        this.providers = providers;
    }

    public static void encode(S2CSyncDatapackDialog packet, FriendlyByteBuf buf) {
        List<DialogReloadListener.NPCDialogProvider> providers = packet.providers;

        buf.writeVarInt(providers.size());

        for (DialogReloadListener.NPCDialogProvider provider : providers) {
            CompoundTag tag = provider.serializeNBT();
            buf.writeNbt(tag);
        }
    }

    public static S2CSyncDatapackDialog decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();

        List<DialogReloadListener.NPCDialogProvider> providers = new ArrayList<>();

        for (int i = 0; i < size; i++) {

            CompoundTag tag = buf.readNbt();

            DialogReloadListener.NPCDialogProvider provider = new DialogReloadListener.NPCDialogProvider();

            if (tag != null) {
                provider.deserializeNBT(tag);
            }

            providers.add(provider);
        }

        return new S2CSyncDatapackDialog(providers);
    }

    public static void handle(S2CSyncDatapackDialog packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {

            handleOnClient(packet.providers);

        });

        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(List<DialogReloadListener.NPCDialogProvider> providers) {

        NPCDialogRegister.clearDatapackDialogs();

        for (DialogReloadListener.NPCDialogProvider provider : providers) {

            ChatSender chatSender = NPCSenderManager.getInstance().getNpcData(provider.getSender());

            if (chatSender == null) {
                continue;
            }

            NPCDialog.Builder builder = new NPCDialog.Builder(provider.getId(), chatSender, provider.isOpenScreen());
            for (DialogReloadListener.NPCDialogActionProvider actionProvider : provider.getActions()) {
                builder.sendMessage(Component.translatable(actionProvider.getMessage()), actionProvider.getOption().isEmpty() ? List.of() : List.of(actionProvider.getOption()));
            }

            builder.buildDatapack();
        }
    }

    public List<DialogReloadListener.NPCDialogProvider> getProviders() {
        return providers;
    }
}
