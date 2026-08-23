package net.shelmarow.mine_chat.chat.picture.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;

public class ChatPicture {
    private final ResourceLocation texture;
    private final boolean isSystem;
    private final int originalWidth;
    private final int originalHeight;

    //gif相关
    private final List<GifFrame> gifFrames = new ArrayList<>();
    private int currentFrame = 0;
    private long lastTime = 0;

    public ChatPicture(ResourceLocation location, int width, int height, boolean isSystem) {
        this.texture = location;
        this.originalWidth = width;
        this.originalHeight = height;
        this.isSystem = isSystem;
    }

    public ChatPicture(List<GifFrame> textures, int width, int height, boolean isSystem) {
        this(textures.get(0).image, width, height, isSystem);
        this.gifFrames.addAll(textures);
    }



    public ResourceLocation getTexture() {
        return texture;
    }

    public ResourceLocation getGifTexture(){
        return gifFrames.get(currentFrame).image;
    }

    public Vec2 getDisplaySize(float limitWidth, float limitHeight) {
        if(originalWidth > limitWidth || originalHeight > limitHeight) {
            float widthScale = limitWidth / originalWidth;
            float heightScale = limitHeight / originalHeight;
            float scale = Math.min(widthScale, heightScale);
            int displayWidth = Math.round(originalWidth * scale);
            int displayHeight = Math.round(originalHeight * scale);
            return new Vec2(displayWidth, displayHeight);
        }
        else {
            return new Vec2(originalWidth, originalHeight);
        }
    }

    public boolean isGif(){
        return !gifFrames.isEmpty();
    }

    public void updateGif() {
        if (isGif()){
            long now = System.currentTimeMillis();
            if (now - lastTime >= gifFrames.get(currentFrame).delay) {
                currentFrame++;
                if (currentFrame >= gifFrames.size()) {
                    currentFrame = 0;
                }
                lastTime = now;
            }
        }
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public ChatPicture copy() {
        ChatPicture chatPicture = new ChatPicture(texture, this.originalWidth, this.originalHeight, isSystem);
        chatPicture.gifFrames.addAll(gifFrames);
        return chatPicture;
    }

    public List<GifFrame> getGifFrames() {
        return gifFrames;
    }
}
