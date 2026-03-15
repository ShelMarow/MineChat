package net.shelmarow.mine_chat.chat.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shelmarow.mine_chat.MineChat;

public class MineChatSounds {

    public static final SoundEvent TYPING;;
    public static final SoundEvent RECEIVE_MESSAGE;

    static {
        TYPING = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, "chat.typing"));
        RECEIVE_MESSAGE = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID,"chat.receive_message"));
    }
}
