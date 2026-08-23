package net.shelmarow.mine_chat.chat.picture.data;

import net.shelmarow.mine_chat.chat.picture.PictureFormat;

public class NetworkPicture {

    private final String hash;
    private final byte[] imageData;
    private final PictureFormat format;

    public NetworkPicture(String hash, byte[] imageData, PictureFormat format) {
        this.hash = hash;
        this.imageData = imageData;
        this.format = format;
    }

    public String getHash() {
        return hash;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public PictureFormat getFormat() {
        return format;
    }

    public int getImageSize() {
        return imageData.length;
    }
}
