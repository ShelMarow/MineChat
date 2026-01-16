package net.shelmarow.mine_chat.chat.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shelmarow.mine_chat.MineChat;

public class MineChatSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS;

    public static final RegistryObject<SoundEvent> TYPING;

    public static final RegistryObject<SoundEvent> RECEIVE_MESSAGE;


    private static RegistryObject<SoundEvent> registerSound(String name) {
        ResourceLocation res = ResourceLocation.fromNamespaceAndPath(MineChat.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(res));
    }

    static {
        SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MineChat.MOD_ID);
        TYPING = registerSound("chat.typing");
        RECEIVE_MESSAGE = registerSound("chat.receive_message");
    }
}
