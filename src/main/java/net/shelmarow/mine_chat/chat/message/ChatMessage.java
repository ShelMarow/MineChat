package net.shelmarow.mine_chat.chat.message;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.message.chat_enum.MessageType;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ChatMessage {
    protected UUID sender;
    protected long timestamp;
    protected int nameLength;
    protected MessageType messageType;
    protected Component message;

    public ChatMessage(UUID sender, long timestamp, int nameLength, MessageType messageType, Component message) {
        this.sender = sender;
        this.timestamp = timestamp;
        this.nameLength = nameLength;
        this.message = message;
        this.messageType = messageType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public UUID getSender() {
        return sender;
    }

    public void setSender(UUID sender) {
        this.sender = sender;
    }

    public Component getMessage() {
        return message;
    }

    public void setMessage(Component message) {
        this.message = message;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public int getNameLength() {
        return nameLength;
    }

    public void setNameLength(int nameLength) {
        this.nameLength = nameLength;
    }
}
