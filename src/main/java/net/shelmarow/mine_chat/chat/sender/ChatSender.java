package net.shelmarow.mine_chat.chat.sender;

import net.minecraft.resources.ResourceLocation;
import net.shelmarow.mine_chat.chat.MineChatManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ChatSender {
    private final @NotNull UUID uuid;
    private final @Nullable String name;
    private final @Nullable ResourceLocation head;
    private boolean customHead = false;
    private final @NotNull SenderType senderType;

    public ChatSender(@NotNull UUID uuid, @Nullable String name, @Nullable ResourceLocation head, @NotNull SenderType senderType) {
        this.uuid = uuid;
        this.name = name;
        this.head = head;
        this.senderType = senderType;
        if(this.senderType == SenderType.NPC) {
            MineChatManager.cacheNPC(this);
        }
    }

    public @NotNull UUID getUuid() {
        return uuid;
    }

    public @Nullable String getName() {
        return name;
    }

    public @Nullable ResourceLocation getHead() {
        return head;
    }

    public @NotNull SenderType getSenderType() {
        return senderType;
    }

    public boolean isCustomHead() {
        return customHead;
    }

    public void setCustomHead(boolean customHead) {
        this.customHead = customHead;
    }
}
