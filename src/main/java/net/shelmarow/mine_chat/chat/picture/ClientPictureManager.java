package net.shelmarow.mine_chat.chat.picture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.data.*;
import net.shelmarow.mine_chat.network.packet.client.C2SRequestPicturePacket;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class ClientPictureManager {

    private static final String TEXTURES_CHAT_PICTURE = "textures/chat_picture/chat";
    private static final String TEXTURES_SYSTEM_PICTURE = "textures/chat_picture/system";
    private static final ClientPictureManager INSTANCE = new ClientPictureManager();

    //玩家可用的表情包和系统专用的图片
    private final Map<String, ChatPicture> pictures = new HashMap<>();
    //用于网络传输的本地图片原始数据
    private final Map<String, NetworkPicture> networkData = new HashMap<>();
    //本地图片显示包装类
    private final Map<String, ChatPicture> localPictures = new HashMap<>();
    private final Map<String, ChatPicture> networkPictures = new HashMap<>();
    //系统图片
    private final Map<String, ChatPicture> systemPictures = new HashMap<>();
    //自定义路径的图片
    private final Map<String, ChatPicture> customPictures = new HashMap<>();

    //服务端是否安装了MineChat
    private boolean isServerInstalled = false;

    private final Set<String> requestedPictures = new HashSet<>();

    private ClientPictureManager() {

    }

    public static ClientPictureManager getInstance() {
        return INSTANCE;
    }

    private static @NotNull ChatPicture creatChatPicture(NativeImage image, ResourceLocation location, boolean system) {
        int width = image.getWidth();
        int height = image.getHeight();
        return new ChatPicture(location, width, height, system);
    }

    public void load(ResourceManager resourceManager) {
        //载入玩家可用的表情图片
        loadChatPicture(resourceManager, pictures, TEXTURES_CHAT_PICTURE);
        //载入系统使用的图片
        loadChatPicture(resourceManager, systemPictures, TEXTURES_SYSTEM_PICTURE);
        //载入本地图片
        loadLocalPicture();
    }

    private void loadLocalPicture() {
        Minecraft mc = Minecraft.getInstance();

        Path root = mc.gameDirectory.toPath().resolve("mine_chat").resolve("pictures");

        if (!Files.exists(root)) {
            MineChat.LOGGER.debug("[MineChat] Local pictures directory does not exist: {}", root);
            return;
        }

        Path localFolder = root.resolve("local");

        // 释放旧的本地图片和网络图片纹理
        releaseTexture(localPictures.values());
        releaseTexture(networkPictures.values());

        // 清空旧数据
        networkData.clear();
        localPictures.clear();
        networkPictures.clear();

        try {
            //保存两个目录下的图片到网络传输中
            try (Stream<Path> pathStream = Files.walk(root)) {
                pathStream.filter(this::isPictureFile).forEach(this::loadNetworkPicture);
            }

            //保存local目录下的图片到自定义表情显示中
            if (Files.isDirectory(localFolder)) {
                try (Stream<Path> pathStream = Files.list(localFolder)) {
                    pathStream.filter(this::isPictureFile).forEach(this::loadLocalPictureFile);
                }
            }

        } catch (IOException e) {
            MineChat.LOGGER.error("[MineChat] Failed to load local pictures", e);
        }

        MineChat.LOGGER.debug("[MineChat] Loaded pictures: network={}, local={}", networkPictures.size(), localPictures.size());
    }

    private boolean isPictureFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);

        return fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".gif");
    }

    private void loadNetworkPicture(Path path) {
        try {
            String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
            boolean isGif = fileName.endsWith(".gif");
            // 根据原始文件计算 hash
            String hash = calculateFileHash(path);
            // 直接读取原始数据
            byte[] imageData = Files.readAllBytes(path);
            NetworkPicture networkPicture = new NetworkPicture(hash, imageData, isGif);
            putNetworkPicture(networkPicture);
            MineChat.LOGGER.debug("[MineChat] Loaded network picture: {} ({} bytes, GIF: {})", hash, imageData.length, isGif);
        } catch (IOException e) {
            MineChat.LOGGER.error("[MineChat] Failed to load network picture: {}", path, e);
        }
    }

    private void loadLocalPictureFile(Path path) {
        try {
            String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);

            if (fileName.endsWith(".png") || fileName.endsWith("jpg")) {
                loadLocalPng(path);
            } else if (fileName.endsWith(".gif")) {
                loadLocalGif(path);
            }

        } catch (Exception e) {
            MineChat.LOGGER.error("[MineChat] Failed to load local picture: {}", path, e);
        }
    }

    private void loadLocalPng(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(inputStream);
            DynamicTexture dynamicTexture = new DynamicTexture(image);
            ResourceLocation texture = Minecraft.getInstance().getTextureManager().register("chat_local_png/" + UUID.randomUUID(), dynamicTexture);
            ChatPicture chatPicture = new ChatPicture(texture, image.getWidth(), image.getHeight(), false);
            String name = calculateFileHash(path);
            localPictures.put(name, chatPicture);
        }
    }

    private void loadLocalGif(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            List<GifFrame> gifFrames = GifDecoder.decodeGif(inputStream);
            if (gifFrames.isEmpty()) {
                MineChat.LOGGER.warn("[MineChat] GIF contains no frames: {}", path);
                return;
            }
            int width = gifFrames.getFirst().width;
            int height = gifFrames.getFirst().height;
            ChatPicture chatPicture = new ChatPicture(gifFrames, width, height, false);
            String name = calculateFileHash(path);
            localPictures.put(name, chatPicture);
        }
    }

    private String getLocalPictureName(Path path) {
        String fileName = path.getFileName().toString();

        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            fileName = fileName.substring(0, dot);
        }

        return fileName;
    }

    public void saveToLocal(NetworkPicture networkPicture) {
        try {
            Minecraft mc = Minecraft.getInstance();

            Path pictureDir = mc.gameDirectory.toPath()
                    .resolve("mine_chat")
                    .resolve("pictures")
                    .resolve("network");

            Files.createDirectories(pictureDir);
            String hash = networkPicture.getHash();
            String extension = networkPicture.isGif() ? ".gif" : ".png";
            Path filePath = pictureDir.resolve(hash + extension);
            byte[] data = networkPicture.getImageData();
            Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            MineChat.LOGGER.warn("[MineChat] Failed to save picture locally: {}", networkPicture.getHash(), e);
        }
    }

    public void putNetworkPicture(NetworkPicture networkPicture) {
        String hash = networkPicture.getHash();
        // 保存原始网络数据
        networkData.put(hash, networkPicture);
        if (networkPicture.isGif()) {
            putGifNetworkPicture(networkPicture);
        }
        else {
            putPngNetworkPicture(networkPicture);
        }
    }

    private void putPngNetworkPicture(NetworkPicture networkPicture) {
        try {
            NativeImage image;
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(networkPicture.getImageData())) {
                image = NativeImage.read(inputStream);
            }

            DynamicTexture dynamicTexture = new DynamicTexture(image);
            ResourceLocation texture = Minecraft.getInstance().getTextureManager().register("chat_png/" + UUID.randomUUID(), dynamicTexture);
            ChatPicture chatPicture = new ChatPicture(texture, image.getWidth(), image.getHeight(), false);
            networkPictures.put(networkPicture.getHash(), chatPicture);
        } catch (IOException e) {
            MineChat.LOGGER.error("[MineChat] Failed to decode PNG network picture: {}", networkPicture.getHash(), e);
            networkData.remove(networkPicture.getHash());
        }
    }

    private void putGifNetworkPicture(NetworkPicture networkPicture) {
        try {
            List<NetworkGifFrame> networkFrames;
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(networkPicture.getImageData())) {
                networkFrames = GifDecoder.decodeGifOriginal(inputStream);
            }

            if (networkFrames.isEmpty()) {
                MineChat.LOGGER.warn("[MineChat] GIF contains no frames: {}", networkPicture.getHash());
                networkData.remove(networkPicture.getHash());
                return;
            }

            List<GifFrame> frames = new ArrayList<>();
            int width = networkFrames.getFirst().getWidth();
            int height = networkFrames.getFirst().getHeight();
            for (NetworkGifFrame frame : networkFrames) {
                DynamicTexture dynamicTexture = new DynamicTexture(frame.getImage());
                ResourceLocation texture = Minecraft.getInstance().getTextureManager().register("chat_gif/" + UUID.randomUUID(), dynamicTexture);
                GifFrame gifFrame = new GifFrame(texture, frame.getDelay(), frame.getWidth(), frame.getHeight());
                frames.add(gifFrame);
            }

            ChatPicture chatPicture = new ChatPicture(frames, width, height, false);

            networkPictures.put(networkPicture.getHash(), chatPicture);

        } catch (Exception e) {
            MineChat.LOGGER.error("[MineChat] Failed to decode GIF network picture: {}", networkPicture.getHash(), e);

            networkData.remove(networkPicture.getHash());
        }
    }

    private String calculateFileHash(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (InputStream is = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Hash algorithm not available", e);
        }
    }


    private void loadChatPicture(ResourceManager resourceManager, Map<String, ChatPicture> pictureMap, String fileLocation) {
        releaseTexture(pictureMap.values());

        pictureMap.clear();

        Map<ResourceLocation, Resource> systemResources = resourceManager.listResources(fileLocation, location -> {
            return location.getPath().endsWith(".png") || location.getPath().endsWith(".gif");
        });

        for (Map.Entry<ResourceLocation, Resource> entry : systemResources.entrySet()) {
            ResourceLocation location = entry.getKey();
            if(location.getPath().endsWith(".png")) {
                Resource resource = entry.getValue();
                String fileName = getFileName(location);
                try (InputStream inputStream = resource.open()) {
                    NativeImage image = NativeImage.read(inputStream);
                    ChatPicture picture = creatChatPicture(image, location, fileLocation.equals(TEXTURES_SYSTEM_PICTURE));
                    pictureMap.put(fileName, picture);
                    image.close();
                } catch (IOException e) {
                    MineChat.LOGGER.error("Failed to load chat picture: {}, {}", location, e);
                }
            }
            else if(location.getPath().endsWith(".gif")) {
                Resource resource = entry.getValue();
                String fileName = getFileName(location);
                try (InputStream inputStream = resource.open()) {
                    List<GifFrame> gifFrames = GifDecoder.decodeGif(inputStream);
                    if(!gifFrames.isEmpty()) {
                        int width = gifFrames.getFirst().width;
                        int height = gifFrames.getFirst().height;
                        ChatPicture picture = new ChatPicture(gifFrames, width, height, fileLocation.equals(TEXTURES_SYSTEM_PICTURE));
                        pictureMap.put(fileName, picture);
                    }

                } catch (IOException e) {
                    MineChat.LOGGER.error("Failed to load chat picture: {}, {}", location, e);
                }
            }
        }
        MineChat.LOGGER.debug("[MineChat] Loaded chat pictures: {}", pictureMap.size());
    }

    private static void releaseTexture(Collection<ChatPicture> chatPictures) {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        for (ChatPicture o : chatPictures) {
            if(o.isGif()){
                for (GifFrame frame : o.getGifFrames()){
                    textureManager.release(frame.image);
                }
            }
            textureManager.release(o.getTexture());
        }
    }

    public @Nullable ChatPicture getPicture(String name) {
        String id = getPictureName(name);
        String type = getPictureType(name);
        ChatPicture picture = null;
        switch (type) {
            case "system" -> {
                picture = systemPictures.get(name);

                if (picture == null) {
                    ResourceLocation location = ResourceLocation.tryParse(id);
                    if (location != null) {
                        picture = getCustomPicture(location);
                    }
                }
            }
            case "chat" -> {
                picture = pictures.get(id);
            }
            case "network" -> {
                picture = localPictures.get(id);
                if(picture == null){
                    picture = networkPictures.get(id);
                }
                //如果本地没有这个图片，尝试向服务器请求
                //因为这个函数调用非常频繁，需要做冷却或者限制请求一次
                if (picture == null && !requestedPictures.contains(id)) {
                    PacketDistributor.sendToServer(new C2SRequestPicturePacket(id));
                    requestedPictures.add(id);
                }
            }
        }

        return picture;
    }

    public @Nullable ChatPicture getCustomPicture(ResourceLocation location) {
        if(customPictures.containsKey(location.toString())) {
            return customPictures.get(location.toString());
        }
        else {
            Optional<Resource> optional = Minecraft.getInstance().getResourceManager().getResource(location);
            if(optional.isPresent()) {
                Resource resource = optional.get();
                try (InputStream inputStream = resource.open()) {
                    NativeImage image = NativeImage.read(inputStream);
                    ChatPicture picture = creatChatPicture(image, location, true);
                    customPictures.put(location.toString(), picture);
                    image.close();
                    return picture;
                } catch (IOException e) {
                    MineChat.LOGGER.error("Failed to load chat picture: {}, {}", location, e);
                }
            }
        }
        return null;
    }

    private String getFileName(ResourceLocation location) {
        String path = location.getPath();
        int index = path.lastIndexOf('/');
        String file = path.substring(index + 1);
        file = file.split("\\.")[0];
        return location.getNamespace() + ":" + file;
    }

    public boolean isPicture(String message) {
        return message.matches("^<MineChatPicture:\\[\"[^\"]+\"]>$");
    }

    public String getPictureID(String message) {
        return message.substring("<MineChatPicture:[\"".length(), message.length() - 3);
    }

    public String getPictureName(String message) {
        return message.split("\\|")[0];
    }

    public String getPictureType(String message) {
        String[] split = message.split("\\|");
        if (split.length < 2) {
            return "chat";
        }
        return split[1];
    }

    public Map<String, ChatPicture> getPictures() {
        return Map.copyOf(pictures);
    }

    public boolean isServerInstalled() {
        return isServerInstalled;
    }

    public void setServerInstalled(boolean serverInstalled) {
        isServerInstalled = serverInstalled;
    }

    public Map<String, NetworkPicture> getNetworkData() {
        return Map.copyOf(networkData);
    }

    public Map<String, ChatPicture> getLocalPictures() {
        return Map.copyOf(localPictures);
    }

    public void clearRequested() {
        requestedPictures.clear();
    }

    public void removeRequested(String hash){
        requestedPictures.remove(hash);
    }

    public void handlePictureRequestResult(String hash, boolean success) {
        if (success) {
            requestedPictures.remove(hash);
            MineChat.LOGGER.debug("[MineChat] Picture request succeeded: {}", hash);
        } else {
            MineChat.LOGGER.debug("[MineChat] Picture request failed: {}", hash);
        }
    }
}