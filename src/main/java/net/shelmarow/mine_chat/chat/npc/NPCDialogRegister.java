package net.shelmarow.mine_chat.chat.npc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.SenderType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class NPCDialogRegister {
    private static final Map<String, Supplier<NPCDialog>> DIALOG_MAP = new HashMap<>();
    private static final Map<String, Supplier<NPCDialog>> DATAPACK_MAP = new HashMap<>();

    public static void init(){
        ChatSender sender = new ChatSender(
                UUID.fromString("381df992-f603-344c-a090-369bad2a924b"),
                "NPC", null, SenderType.NPC);

        new NPCDialog.Builder("test", sender, true)
                .sendMessage(p-> Component.translatable("你好 ").append(p.getDisplayName()))
                .sendMessage(Component.translatable("这是一条NPC消息"))
                .sendMessage(Component.translatable("你可以点击按钮回复"), List.of("好的"), p->{
                    if(p instanceof ServerPlayer serverPlayer){
                        serverPlayer.displayClientMessage(Component.translatable("服务端回调"), false);
                    }
                    else {
                        p.displayClientMessage(Component.translatable("客户端回调"), false);
                    }
                })
                .sendMessage(Component.translatable("做的好！"))
                .sendMessage(Component.translatable("对话到此为止"))
                .build();
    }

    public static void registerNPCDialog(String name, Supplier<NPCDialog> dialog) {
        DIALOG_MAP.put(name, dialog);
    }

    public static void registerDatapackDialog(String name, Supplier<NPCDialog> dialog) {
        DATAPACK_MAP.put(name, dialog);
    }

    public static void clearDatapackDialogs() {
        DATAPACK_MAP.clear();
    }

    public static @Nullable NPCDialog getNPCDialog(String name) {
        if(DIALOG_MAP.containsKey(name)){
            return DIALOG_MAP.get(name).get();
        }
        if(DATAPACK_MAP.containsKey(name)){
            return DATAPACK_MAP.get(name).get();
        }
        return null;
    }

    public static List<String> listID() {
        ArrayList<String> arrayList = new ArrayList<>(DIALOG_MAP.keySet());
        arrayList.addAll(DATAPACK_MAP.keySet());
        return arrayList;
    }
}
