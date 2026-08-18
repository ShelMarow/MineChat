package net.shelmarow.mine_chat.chat.npc;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class NPCDialogRegister {
    private static final Map<String, Supplier<NPCDialog>> DIALOG_MAP = new HashMap<>();

    public static void init(){}

    public static void registerNPCDialog(String name, Supplier<NPCDialog> dialog) {
        DIALOG_MAP.put(name, dialog);
    }

    public static @Nullable NPCDialog getNPCDialog(String name) {
        if(DIALOG_MAP.containsKey(name)){
            NPCDialog npcDialog = DIALOG_MAP.get(name).get();
            npcDialog.setDialogID(name);
            return npcDialog;
        }
        return null;
    }

    public static List<String> listID() {
        return new ArrayList<>(DIALOG_MAP.keySet());
    }
}
