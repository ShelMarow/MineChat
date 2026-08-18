package net.shelmarow.mine_chat.chat.picture.data;

import com.mojang.blaze3d.platform.NativeImage;

public class NetworkGifFrame {
    public final int delay;
    public final int width;
    public final int height;
    private final NativeImage image;
    private final String disposalMethod;

    public NetworkGifFrame(NativeImage image, int delay, int width, int height, String disposalMethod) {
        this.image = image;
        this.delay = delay;
        this.width = width;
        this.height = height;
        this.disposalMethod = disposalMethod;
    }


    public NativeImage getImage() {
        return image;
    }

    public int getDelay() {
        return delay;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getDisposalMethod() {
        return disposalMethod;
    }
}
