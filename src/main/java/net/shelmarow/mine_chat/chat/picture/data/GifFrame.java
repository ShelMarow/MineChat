package net.shelmarow.mine_chat.chat.picture.data;

import net.minecraft.resources.ResourceLocation;

public class GifFrame {
    public final ResourceLocation image;
    public final int delay;
    public final int width;
    public final int height;

    public GifFrame(ResourceLocation image, int delay, int width, int height) {
        this.image = image;
        this.delay = delay;
        this.width = width;
        this.height = height;
    }
}
