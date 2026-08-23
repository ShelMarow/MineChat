package net.shelmarow.mine_chat.chat.message;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.chat.message.chat_enum.MessageType;
import net.shelmarow.mine_chat.chat.sender.ChatSender;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ChatMessage {
    protected ChatSender sender;
    protected long timestamp;
    protected int nameLength;
    protected MessageType messageType;
    protected Component message;

    public ChatMessage(ChatSender sender, long timestamp, int nameLength, MessageType messageType, Component message) {
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

    public ChatSender getSender() {
        return sender;
    }

    public void setSender(ChatSender sender) {
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

    public SenderWithMessage getDisplayMessage() {
        //消息
        Component text = message;

        //确定要渲染的文本总高度
        MutableComponent senderName = Component.empty();
        MutableComponent finalMessage = Component.empty();

        //将名字单独分割出来
        List<Component> lists = text.toFlatList();
        for(int i = 0; i < lists.size(); i++){
            if(i < nameLength){
                senderName.append(lists.get(i));
            }
            else{
                finalMessage.append(lists.get(i));
            }
        }

        return new SenderWithMessage(senderName, finalMessage);
    }



    public record SenderWithMessage(MutableComponent senderName, MutableComponent finalMessage) {

    }
}
