package net.shelmarow.mine_chat.network.packet.server;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.mine_chat.chat.npc.ClientDialogProcessHandler;
import net.shelmarow.mine_chat.chat.npc.NPCDialog;
import net.shelmarow.mine_chat.chat.npc.NPCDialogRegister;
import net.shelmarow.mine_chat.chat.npc.action.DialogAction;
import net.shelmarow.mine_chat.chat.screen.NPCCallScreen;

import java.util.function.Supplier;

public class S2CSendActionPacket {

    private final String dialogID;
    private final int currentIndex;
    private final boolean first;
    private final boolean forceOpen;

    public S2CSendActionPacket(String dialogID, int currentIndex, boolean first, boolean forceOpen) {
        this.dialogID = dialogID;
        this.currentIndex = currentIndex;
        this.first = first;
        this.forceOpen = forceOpen;
    }

    public static S2CSendActionPacket decode(FriendlyByteBuf buf) {
        return new S2CSendActionPacket(buf.readUtf(), buf.readInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(S2CSendActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            handleOnClient(packet);
        });

        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(S2CSendActionPacket packet) {
        NPCDialog npcDialog = NPCDialogRegister.getNPCDialog(packet.dialogID);

        if (npcDialog != null) {
            DialogAction dialogAction = npcDialog.getActions().get(packet.currentIndex).get();

            ClientDialogProcessHandler.getInstance().addDialogActionQuest(npcDialog.getChatSender().getUuid(), dialogAction, packet.first);

            if (packet.first && packet.forceOpen) {
                Minecraft.getInstance().setScreen(new NPCCallScreen(npcDialog.getChatSender()));
            }
        }
    }

    public String getDialogID() {
        return dialogID;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isForceOpen() {
        return forceOpen;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(dialogID);
        buf.writeInt(currentIndex);
        buf.writeBoolean(first);
        buf.writeBoolean(forceOpen);
    }
}