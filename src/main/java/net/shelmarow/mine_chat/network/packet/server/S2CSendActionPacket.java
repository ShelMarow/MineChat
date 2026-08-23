package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.npc.ClientDialogProcessHandler;
import net.shelmarow.mine_chat.chat.npc.NPCDialog;
import net.shelmarow.mine_chat.chat.npc.NPCDialogRegister;
import net.shelmarow.mine_chat.chat.npc.action.DialogAction;
import net.shelmarow.mine_chat.chat.screen.NPCCallScreen;
import org.jetbrains.annotations.NotNull;

public record S2CSendActionPacket(String dialogID, int currentIndex, boolean first, boolean forceOpen) implements CustomPacketPayload {

    public static final Type<S2CSendActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    MineChat.MOD_ID,
                    "send_action_packet"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSendActionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    S2CSendActionPacket::dialogID,
                    ByteBufCodecs.INT,
                    S2CSendActionPacket::currentIndex,
                    ByteBufCodecs.BOOL,
                    S2CSendActionPacket::first,
                    ByteBufCodecs.BOOL,
                    S2CSendActionPacket::forceOpen,
                    S2CSendActionPacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CSendActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            handleOnClient(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(S2CSendActionPacket packet) {
        NPCDialog npcDialog = NPCDialogRegister.getNPCDialog(packet.dialogID());
        if(npcDialog != null) {
            DialogAction dialogAction = npcDialog.getActions().get(packet.currentIndex()).get();
            ClientDialogProcessHandler.getInstance().addDialogActionQuest(
                    npcDialog.getChatSender().getUuid(), dialogAction, packet.first()
            );
            if(packet.first() && packet.forceOpen()){
                Minecraft.getInstance().setScreen(new NPCCallScreen(npcDialog.getChatSender()));
            }
        }
    }
}
