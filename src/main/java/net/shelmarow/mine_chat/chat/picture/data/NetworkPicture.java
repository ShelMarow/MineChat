package net.shelmarow.mine_chat.chat.picture.data;

public class NetworkPicture {

    private final String hash;
    private final byte[] imageData;
    private final boolean isGif;

    public NetworkPicture(String hash, byte[] imageData, boolean isGif) {
        this.hash = hash;
        this.imageData = imageData;
        this.isGif = isGif;
    }

    public String getHash() {
        return hash;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public boolean isGif() {
        return isGif;
    }

    public int getImageSize() {
        return imageData.length;
    }
}
