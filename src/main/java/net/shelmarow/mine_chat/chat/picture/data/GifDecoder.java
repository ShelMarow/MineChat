package net.shelmarow.mine_chat.chat.picture.data;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GifDecoder {

    public static List<NetworkGifFrame> decodeGifOriginal(InputStream inputStream) {
        List<NetworkGifFrame> frames = new ArrayList<>();
        try {
            List<DecodedFrame> decodedFrames = decodeFrames(inputStream);
            for (DecodedFrame frame : decodedFrames) {
                frames.add(new NetworkGifFrame(frame.image(), frame.delay(), frame.width(), frame.height(), frame.disposalMethod()));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return frames;
    }

    public static List<GifFrame> decodeGif(InputStream inputStream) {
        List<GifFrame> frames = new ArrayList<>();
        try {
            List<DecodedFrame> decodedFrames = decodeFrames(inputStream);
            for (DecodedFrame frame : decodedFrames) {
                DynamicTexture dynamicTexture = new DynamicTexture(frame.image());
                ResourceLocation texture = Minecraft.getInstance().getTextureManager().register("gif/" + UUID.randomUUID(), dynamicTexture);
                frames.add(new GifFrame(texture, frame.delay(), frame.width(), frame.height()));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return frames;
    }

    private static List<DecodedFrame> decodeFrames(InputStream inputStream) throws Exception {
        List<DecodedFrame> frames = new ArrayList<>();
        try (ImageInputStream stream = ImageIO.createImageInputStream(inputStream)) {
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            try {
                reader.setInput(stream, false);
                int width;
                int height;
                IIOMetadata streamMetadata = reader.getStreamMetadata();
                if (streamMetadata != null) {
                    String metaFormat = streamMetadata.getNativeMetadataFormatName();
                    IIOMetadataNode root = (IIOMetadataNode) streamMetadata.getAsTree(metaFormat);
                    IIOMetadataNode screenDescriptor = (IIOMetadataNode) root.getElementsByTagName("LogicalScreenDescriptor").item(0);
                    width = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenWidth"));
                    height = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenHeight"));
                } else {
                    BufferedImage firstImage = reader.read(0);
                    width = firstImage.getWidth();
                    height = firstImage.getHeight();
                }
                BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                String previousDisposal = "none";
                int previousX = 0;
                int previousY = 0;
                int previousWidth = 0;
                int previousHeight = 0;
                BufferedImage previousCanvas = null;
                int numImages = reader.getNumImages(true);
                for (int i = 0; i < numImages; i++) {
                    if (i > 0) {
                        applyDisposal(canvas, previousCanvas, previousDisposal, previousX, previousY, previousWidth, previousHeight);
                    }
                    BufferedImage bufferedImage = reader.read(i);
                    IIOMetadata metadata = reader.getImageMetadata(i);
                    String metaFormat = metadata.getNativeMetadataFormatName();
                    IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormat);
                    int frameX = 0;
                    int frameY = 0;
                    int frameWidth = bufferedImage.getWidth();
                    int frameHeight = bufferedImage.getHeight();
                    IIOMetadataNode imageDescriptor = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
                    if (imageDescriptor != null) {
                        frameX = Integer.parseInt(imageDescriptor.getAttribute("imageLeftPosition"));
                        frameY = Integer.parseInt(imageDescriptor.getAttribute("imageTopPosition"));
                    }


                    int delay = 100;
                    String disposalMethod = "none";
                    IIOMetadataNode gceNode = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);

                    if (gceNode != null) {
                        String delayString = gceNode.getAttribute("delayTime");
                        if (!delayString.isEmpty()) {
                            delay = Integer.parseInt(delayString) * 10;
                        }
                        String disposal = gceNode.getAttribute("disposalMethod");
                        if (!disposal.isEmpty()) {
                            disposalMethod = disposal;
                        }
                    }
                    if (delay <= 0) {
                        delay = 50;
                    }

                    if ("restoreToPrevious".equals(disposalMethod)) {
                        previousCanvas = copyImage(canvas);
                    } else {
                        previousCanvas = null;
                    }
                    Graphics2D graphics = canvas.createGraphics();
                    try {
                        graphics.setComposite(AlphaComposite.SrcOver);
                        graphics.drawImage(bufferedImage, frameX, frameY, null);
                    } finally {
                        graphics.dispose();
                    }
                    NativeImage nativeImage = convertToNativeImage(canvas);
                    frames.add(new DecodedFrame(nativeImage, delay, width, height, disposalMethod));
                    previousDisposal = disposalMethod;
                    previousX = frameX;
                    previousY = frameY;
                    previousWidth = frameWidth;
                    previousHeight = frameHeight;
                }
            } finally {
                reader.dispose();
            }
        }
        return frames;
    }

    private static void applyDisposal(BufferedImage canvas, BufferedImage previousCanvas, String previousDisposal, int previousX, int previousY, int previousWidth, int previousHeight) {
        switch (previousDisposal) {
            case "restoreToBackgroundColor" -> clearRect(canvas, previousX, previousY, previousWidth, previousHeight);
            case "restoreToPrevious" -> {
                if (previousCanvas != null) {
                    Graphics2D graphics = canvas.createGraphics();
                    try {
                        graphics.setComposite(AlphaComposite.Src);
                        graphics.drawImage(previousCanvas, 0, 0, null);
                    } finally {
                        graphics.dispose();
                    }
                }
            }
        }
    }

    private static void clearRect(BufferedImage image, int x, int y, int width, int height) {
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(x, y, width, height);
        } finally {
            graphics.dispose();
        }
    }

    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static NativeImage convertToNativeImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int rgba = r | (g << 8) | (b << 16) | (a << 24);
                nativeImage.setPixelRGBA(x, y, rgba);
            }
        }
        return nativeImage;
    }

    private record DecodedFrame(NativeImage image, int delay, int width, int height,String disposalMethod) {}
}