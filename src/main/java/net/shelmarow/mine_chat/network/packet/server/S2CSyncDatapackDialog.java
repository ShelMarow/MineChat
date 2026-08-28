package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.datapack.DialogReloadListener;
import net.shelmarow.mine_chat.chat.npc.NPCDialog;
import net.shelmarow.mine_chat.chat.npc.NPCDialogRegister;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.NPCSenderManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record S2CSyncDatapackDialog(List<DialogReloadListener.NPCDialogProvider> providers) implements CustomPacketPayload {


    public static final Type<S2CSyncDatapackDialog> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "sync_datapack_dialog"
            ));


    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncDatapackDialog> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> {
                buf.writeVarInt(packet.providers.size());
                for (DialogReloadListener.NPCDialogProvider provider : packet.providers()){
                    buf.writeNbt(provider.serializeNBT());
                }

            }, (buf) -> {
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
            });


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CSyncDatapackDialog packet , IPayloadContext context){
        context.enqueueWork(() -> {
            handleOnClient(packet.providers());
        });
    }


    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(List<DialogReloadListener.NPCDialogProvider> providers){
        NPCDialogRegister.clearDatapackDialogs();
        for(DialogReloadListener.NPCDialogProvider provider : providers){
            ChatSender chatSender = NPCSenderManager.getInstance().getNpcData(provider.getSender());
            if(chatSender != null){
                NPCDialog.Builder builder = new NPCDialog.Builder(provider.getId(), chatSender, provider.isOpenScreen());
                for (DialogReloadListener.NPCDialogActionProvider actionProvider : provider.getActions()){
                    builder.sendMessage(
                            Component.translatable(actionProvider.getMessage()),
                            actionProvider.getOption().isEmpty() ? List.of() : List.of(actionProvider.getOption()));
                }
                builder.buildDatapack();
            }
        }
    }
}
