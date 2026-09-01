package net.shelmarow.mine_chat.chat.picture;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class ClientPictureManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CUSTOM_PICTURE_FOLDER = "mine_chat";
    private static final String CUSTOM_PICTURE_CONFIG = "custom_pictures.json";

    private static final String TEXTURES_CHAT_PICTURE = "textures/chat_picture/chat";
    private static final String TEXTURES_SYSTEM_PICTURE = "textures/chat_picture/system";
    private static final ClientPictureManager INSTANCE = new ClientPictureManager();

    private static final Pattern PICTURE_PATTERN = Pattern.compile("<MineChatPicture:\\[\"([^\"]+)\"]>");

    //玩家可用的表情包和系统专用的图片
    private final Map<String, ChatPicture> pictures = new LinkedHashMap<>();
    //表情分组
    private final Map<String, Map<String, ChatPicture>> pictureGroups = new LinkedHashMap<>();

    //系统图片
    private final Map<String, ChatPicture> systemPictures = new LinkedHashMap<>();
    //自定义路径的系统图片
    private final Map<String, ChatPicture> customSystemPictures = new LinkedHashMap<>();

    //用于网络传输的本地图片原始数据
    private final Map<String, NetworkPicture> networkData = new LinkedHashMap<>();
    //保存的网络图片显示包装类
    private final Map<String, ChatPicture> networkPictures = new LinkedHashMap<>();

    //向服务器请求的图片hash值
    private final Set<String> requestedPictures = new HashSet<>();



    //自定义表情包的路径合集
    private final Set<Path> customPicturePaths = new HashSet<>();
    //用于渲染的自定义表情包
    private final Map<String, ChatPicture> customPictures = new LinkedHashMap<>();
    //自定义表情包的图片和对应的路径
    private final Map<Path, Set<String>> customPathPictureHashes = new HashMap<>();

    //异步加载图片，防止卡顿
    private final Queue<Path> customPictureLoadQueue = new ArrayDeque<>();
    private static final long CUSTOM_PNG_PIXEL_BUDGET = 256 * 1024L;
    private boolean customPictureLoading = false;

    private int customPictureLoadTotal = 0;
    private int customPictureLoadProgress = 0;
    private int lastCustomPictureProgress = -1;


    private ClientPictureManager() {}

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
        loadModChatPicture(resourceManager, pictures, TEXTURES_CHAT_PICTURE);

        //载入系统使用的图片
        loadModChatPicture(resourceManager, systemPictures, TEXTURES_SYSTEM_PICTURE);

        //载入本地图片
        loadNetworkPictures();
        loadCustomPictures(Minecraft.getInstance().level != null);

        MineChat.LOGGER.debug("[MineChat] Loaded pictures: network={}", networkPictures.size());
    }



    //读取静态图片
    public @Nullable NativeImage readStaticPicture(byte[] data) {
        try {
            if (data.length == 0) {
                return null;
            }

            PictureFormat format = detectPictureFormat(new ByteArrayInputStream(data));
            if (format == PictureFormat.PNG) {
                try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
                    return NativeImage.read(stream);
                }
            }

            else if (format == PictureFormat.JPG) {
                return readJpgPicture(data);
            }

        } catch (IOException e) {
            MineChat.LOGGER.error("[MineChat] Failed to read static picture", e);
        }

        return null;
    }

    //非png需要额外转换
    private @Nullable NativeImage readJpgPicture(byte[] data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            BufferedImage bufferedImage = ImageIO.read(inputStream);

            if (bufferedImage == null) {
                MineChat.LOGGER.warn("[MineChat] Failed to decode JPG image");
                return null;
            }

            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();

            NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = bufferedImage.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int rgba = (r) | (g << 8) | (b << 16) | (a << 24);
                    nativeImage.setPixelRGBA(x, y, rgba);
                }
            }
            bufferedImage.flush();
            return nativeImage;
        } catch (Exception e) {
            MineChat.LOGGER.error("[MineChat] Failed to decode JPG image", e);
            return null;
        }
    }

    //读取模组目录下的图
    private void loadModChatPicture(ResourceManager resourceManager, Map<String, ChatPicture> pictureMap, String fileLocation) {
        releaseTexture(pictureMap.values());
        pictureMap.clear();

        boolean isChatPicture = fileLocation.equals(TEXTURES_CHAT_PICTURE);

        if (isChatPicture) {
            pictureGroups.clear();
        }

        Map<ResourceLocation, Resource> resources = resourceManager.listResources(fileLocation,
                location -> location.getPath().endsWith(".png") || location.getPath().endsWith(".jpg") || location.getPath().endsWith(".gif")
        );

        List<ResourceLocation> sortedLocations = new ArrayList<>(resources.keySet());
        sortedLocations.sort(Comparator.comparing(ResourceLocation::getPath));

        for (ResourceLocation location : sortedLocations) {
            Resource resource = resources.get(location);
            String fileName = getFileName(location);

            if (location.getPath().endsWith(".png") || location.getPath().endsWith(".jpg")) {
                try (InputStream inputStream = resource.open()) {
                    byte[] bytes = inputStream.readAllBytes();
                    NativeImage image = readStaticPicture(bytes);

                    if(image != null) {
                        ChatPicture picture = creatChatPicture(image, location, fileLocation.equals(TEXTURES_SYSTEM_PICTURE));
                        pictureMap.put(fileName, picture);
                        if (isChatPicture) {
                            String group = getPictureGroup(location, fileLocation);
                            pictureGroups.computeIfAbsent(group, k -> new LinkedHashMap<>()).put(fileName, picture);
                        }
                        image.close();
                    }
                } catch (IOException e) {
                    MineChat.LOGGER.error("Failed to load chat picture: {}, {}", location, e);
                }

            } else if (location.getPath().endsWith(".gif")) {
                try (InputStream inputStream = resource.open()) {
                    List<GifFrame> gifFrames = GifDecoder.decodeGif(inputStream);

                    if (!gifFrames.isEmpty()) {
                        int width = gifFrames.getFirst().width;
                        int height = gifFrames.getFirst().height;

                        ChatPicture picture = new ChatPicture(gifFrames, width, height, fileLocation.equals(TEXTURES_SYSTEM_PICTURE));

                        pictureMap.put(fileName, picture);

                        if (isChatPicture) {
                            String group = getPictureGroup(location, fileLocation);
                            pictureGroups.computeIfAbsent(group, k -> new LinkedHashMap<>()).put(fileName, picture);
                        }
                    }
                } catch (IOException e) {
                    MineChat.LOGGER.error("Failed to load chat picture: {}, {}", location, e);
                }
            }
        }

        MineChat.LOGGER.debug("[MineChat] Loaded chat pictures: {}, groups: {}", pictureMap.size(), isChatPicture ? pictureGroups.size() : 0);
    }


    //保存网络通信使用的图片
    public void loadNetworkPictures() {
        Minecraft mc = Minecraft.getInstance();

        Path root = mc.gameDirectory.toPath().resolve("mine_chat").resolve("pictures").resolve("network");

        releaseTexture(networkPictures.values());
        networkPictures.clear();
        networkData.clear();

        if (!Files.isDirectory(root)) {
            MineChat.LOGGER.debug("[MineChat] Network pictures directory does not exist: {}", root);
            return;
        }

        try (Stream<Path> pathStream = Files.list(root)) {
            pathStream.sorted(Comparator.comparing(Path::toString)).forEach(path -> {
                PictureFormat format = detectPictureFormat(path);
                if (format != PictureFormat.UNKNOWN) {
                    loadNetworkPicture(path, format);
                }
            });
        } catch (IOException e) {
            MineChat.LOGGER.error("[MineChat] Failed to load network pictures", e);
        }

        MineChat.LOGGER.debug("[MineChat] Loaded network pictures: {}", networkPictures.size());
    }

    private void loadNetworkPicture(Path path, PictureFormat format) {
        try {
            // 直接读取原始数据
            byte[] imageData = Files.readAllBytes(path);
            // 根据原始文件计算 hash
            String hash = calculateFileHash(imageData);
            NetworkPicture networkPicture = new NetworkPicture(hash, imageData, format);
            putNetworkPicture(networkPicture);
            MineChat.LOGGER.debug("[MineChat] Loaded network picture: {} ({} bytes, GIF: {})", hash, imageData.length, format);
        } catch (IOException e) {
            MineChat.LOGGER.error("[MineChat] Failed to load network picture: {}", path, e);
        }
    }

    public void putNetworkPicture(NetworkPicture networkPicture) {
        String hash = networkPicture.getHash();
        // 保存原始网络数据
        networkData.put(hash, networkPicture);
        if (networkPicture.getFormat() == PictureFormat.GIF) {
            putGifNetworkPicture(networkPicture);
        }
        else {
            putStaticNetworkPicture(networkPicture);
        }
    }

    private void putStaticNetworkPicture(NetworkPicture networkPicture) {
        NativeImage image = readStaticPicture(networkPicture.getImageData());
        if(image != null) {
            DynamicTexture dynamicTexture = new DynamicTexture(image);
            ResourceLocation texture = Minecraft.getInstance().getTextureManager().register("chat_png/" + UUID.randomUUID(), dynamicTexture);
            ChatPicture chatPicture = new ChatPicture(texture, image.getWidth(), image.getHeight(), false);
            networkPictures.put(networkPicture.getHash(), chatPicture);
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



    //读取自定义文件目录下的图片
    private void loadCustomPictures(boolean async) {
        loadCustomPicturePaths();

        customPictures.clear();

        customPictureLoadQueue.clear();
        customPictureLoading = false;

        customPictureLoadTotal = 0;
        customPictureLoadProgress = 0;
        lastCustomPictureProgress = -1;

        for (Path path : customPicturePaths) {

            if (!Files.exists(path)) {
                MineChat.LOGGER.warn("[MineChat] Custom picture path does not exist: {}", path);
                continue;
            }

            try {
                if (Files.isDirectory(path)) {
                    try (Stream<Path> stream = Files.walk(path)) {
                        stream.filter(Files::isRegularFile)
                                .filter(this::isPictureFile)
                                .sorted(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER))
                                .forEach(customPictureLoadQueue::offer);
                    }

                } else if (Files.isRegularFile(path) && isPictureFile(path)) {
                    customPictureLoadQueue.offer(path);
                }

            } catch (Exception e) {
                MineChat.LOGGER.error("[MineChat] Failed to scan custom picture path: {}", path, e);
            }
        }

        customPictureLoadTotal = customPictureLoadQueue.size();

        if (customPictureLoadTotal == 0) {
            MineChat.LOGGER.debug("[MineChat] No custom pictures to load");
            return;
        }

        /*
         * 已经进入世界：
         * 使用 Tick 分步加载。
         */
        if (async) {
            customPictureLoading = true;
            MineChat.LOGGER.info("[MineChat] Start async loading {} custom pictures", customPictureLoadTotal);
        }
        else {
            MineChat.LOGGER.info("[MineChat] Start synchronous loading {} custom pictures", customPictureLoadTotal);

            while (!customPictureLoadQueue.isEmpty()) {
                Path picturePath = customPictureLoadQueue.poll();

                if (picturePath == null) {
                    continue;
                }

                loadCustomPictureFile(picturePath);

                customPictureLoadProgress++;

                if (customPictureLoadTotal > 0) {
                    int progress = customPictureLoadProgress * 100 / customPictureLoadTotal;

                    MineChat.LOGGER.debug("[MineChat] Loading custom pictures: {}/{} ({}%)", customPictureLoadProgress, customPictureLoadTotal, progress);
                }
            }

            customPictureLoading = false;
            MineChat.LOGGER.info("[MineChat] Finished loading custom pictures: {}/{}", customPictureLoadProgress, customPictureLoadTotal);

        }
    }

    //读取保存的自定义目录
    private void loadCustomPicturePaths() {
        customPicturePaths.clear();
        Path configPath = getCustomPictureConfigPath();

        if (!Files.exists(configPath)) {
            return;
        }

        try {
            String json = Files.readString(configPath);
            if (json.isBlank()) {
                return;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray paths = root.getAsJsonArray("paths");

            if (paths == null) {
                return;
            }

            for (var element : paths) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }

                String pathString = element.getAsString();

                if (pathString == null || pathString.isBlank()) {
                    continue;
                }

                Path path;
                try {
                    path = Path.of(pathString);
                } catch (Exception e) {
                    MineChat.LOGGER.warn("[MineChat] Invalid custom picture path: {}", pathString);
                    continue;
                }
                customPicturePaths.add(path);
            }
            MineChat.LOGGER.debug("[MineChat] Loaded {} custom picture paths", customPicturePaths.size());
        } catch (Exception e) {
            MineChat.LOGGER.error("[MineChat] Failed to load custom picture config", e);
        }
    }

    private void loadCustomPictureFile(Path path) {
        try {
            byte[] data = Files.readAllBytes(path);

            String hash = calculateFileHash(data);
            if (customPictures.containsKey(hash)) {
                return;
            }

            PictureFormat format = detectPictureFormat(new ByteArrayInputStream(data));
            NetworkPicture networkPicture = new NetworkPicture(hash, data, format);
            networkData.put(hash, networkPicture);

            if (format == PictureFormat.GIF) {
                loadCustomGif(hash, data, path);
            } else {
                loadCustomStaticPicture(hash, data, path);
            }

            MineChat.LOGGER.debug("[MineChat] Loaded custom picture: {} -> {}", hash, path);


        } catch (Exception e) {

            MineChat.LOGGER.error("[MineChat] Failed to load custom picture: {}", path, e);
        }
    }

    private void loadCustomGif(String hash, byte[] data, Path path) {
        List<GifFrame> gifFrames;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            gifFrames = GifDecoder.decodeGif(inputStream);
        }
        catch (IOException e) {
            MineChat.LOGGER.error("[MineChat] Failed to load custom gif: {}", path, e);
            return;
        }

        if (gifFrames.isEmpty()) {
            MineChat.LOGGER.warn("[MineChat] Custom GIF contains no frames: {}", hash);
            networkData.remove(hash);
            return;
        }

        int width = gifFrames.getFirst().width;
        int height = gifFrames.getFirst().height;
        ChatPicture chatPicture = new ChatPicture(gifFrames, width, height, false);
        //networkPictures.put(hash, chatPicture);
        customPictures.put(hash, chatPicture);
        customPathPictureHashes.computeIfAbsent(path, k-> new HashSet<>()).add(hash);
    }

    private void loadCustomStaticPicture(String hash, byte[] data, Path path) {
        NativeImage image = readStaticPicture(data);
        if(image != null) {
            DynamicTexture dynamicTexture = new DynamicTexture(image);
            ResourceLocation texture = Minecraft.getInstance().getTextureManager().register("chat_custom/" + UUID.randomUUID(), dynamicTexture);
            ChatPicture chatPicture = new ChatPicture(texture, image.getWidth(), image.getHeight(), false);
            //networkPictures.put(hash, chatPicture);
            customPictures.put(hash, chatPicture);
            customPathPictureHashes.computeIfAbsent(path, k-> new HashSet<>()).add(hash);
        }
    }

    public void tickCustomPictureLoading() {
        if (!customPictureLoading) {
            return;
        }

        long usedPixels = 0;

        while (!customPictureLoadQueue.isEmpty()) {

            Path path = customPictureLoadQueue.peek();

            if (path == null) {
                customPictureLoadQueue.poll();
                customPictureLoadProgress++;
                continue;
            }


            long cost;

            try (InputStream inputStream = Files.newInputStream(path)){
                cost = inputStream.readAllBytes().length;
            }
            catch (Exception e) {
                customPictureLoadQueue.poll();
                customPictureLoadProgress++;
                continue;
            }

            if (usedPixels > 0 && usedPixels + cost > CUSTOM_PNG_PIXEL_BUDGET) {
                break;
            }

            customPictureLoadQueue.poll();
            loadCustomPictureFile(path);

            customPictureLoadProgress++;
            usedPixels += cost;

            printCustomPictureLoadProgress();

            if (usedPixels >= CUSTOM_PNG_PIXEL_BUDGET) {
                break;
            }
        }

        if (customPictureLoadQueue.isEmpty()) {
            customPictureLoading = false;
            customPictureLoadProgress = customPictureLoadTotal;

            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("mine_chat.picture.custom_loading.finished", customPictureLoadProgress, customPictureLoadTotal), false
                );
            }
            MineChat.LOGGER.info("[MineChat] Finished tick loading custom pictures: {}/{}", customPictureLoadProgress, customPictureLoadTotal);
        }
    }


    //加载拖动进窗口的图片
    public void loadDropFiles(@NotNull List<Path> paths) {
        List<Path> sorted = new ArrayList<>();

        for (Path path : paths) {
            if(Files.isDirectory(path)){
                try (Stream<Path> stream = Files.walk(path)) {
                    stream.filter(Files::isRegularFile)
                            .filter(this::isPictureFile)
                            .sorted(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER))
                            .forEach(sorted::add);
                }
                catch (IOException e) {
                    MineChat.LOGGER.error("[MineChat] Failed to load dorp picture: {}", path, e);
                }
            }
            else {
                sorted.add(path);
            }
        }

        int counter = 0;
        int failed = 0;
        for(Path path : sorted){
            PictureFormat format = detectPictureFormat(path);
            if(format != PictureFormat.UNKNOWN){
                addCustomPicturePath(path);
                counter++;
            }
            else {
                failed++;
            }
        }

        if(counter > 0){
            if(Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("mine_chat.picture.load_result", counter, failed), false);
            }
            MineChat.LOGGER.debug("[MineChat] Loaded {} local picture files", counter);
        }
        else {
            if(Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.translatable( "mine_chat.picture.load_failed", counter), false);
            }
        }
    }



    private PictureFormat detectPictureFormat(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return detectPictureFormat(inputStream);
        } catch (IOException e) {
            MineChat.LOGGER.warn("[MineChat] Failed to detect picture format: {}", path);
            return PictureFormat.UNKNOWN;
        }
    }

    private @NotNull PictureFormat detectPictureFormat(InputStream inputStream) throws IOException {
        byte[] header = inputStream.readNBytes(8);

        // PNG
        if (header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && (header[1] & 0xFF) == 0x50
                && (header[2] & 0xFF) == 0x4E
                && (header[3] & 0xFF) == 0x47
                && (header[4] & 0xFF) == 0x0D
                && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A
                && (header[7] & 0xFF) == 0x0A) {

            return PictureFormat.PNG;
        }

        // GIF87a / GIF89a
        if (header.length >= 6
                && header[0] == 'G'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == '8'
                && (header[4] == '7' || header[4] == '9')
                && header[5] == 'a') {

            return PictureFormat.GIF;
        }

        // JPG / JPEG
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {

            return PictureFormat.JPG;
        }

        return PictureFormat.UNKNOWN;
    }

    private String calculateFileHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(data);

            StringBuilder hexString = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }


    public void saveToNetworkFile(NetworkPicture networkPicture) {
        try {
            Minecraft mc = Minecraft.getInstance();

            Path pictureDir = mc.gameDirectory.toPath()
                    .resolve("mine_chat")
                    .resolve("pictures")
                    .resolve("network");

            Files.createDirectories(pictureDir);
            String hash = networkPicture.getHash();
            String extension = ".png";
            switch (networkPicture.getFormat()){
                case GIF -> extension = ".gif";
                case JPG -> extension = ".jpg";
                case PNG -> extension = ".png";
            }
            Path filePath = pictureDir.resolve(hash + extension);
            byte[] data = networkPicture.getImageData();
            Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            MineChat.LOGGER.warn("[MineChat] Failed to save picture locally: {}", networkPicture.getHash(), e);
        }
    }



    private String getPictureGroup(ResourceLocation location, String fileLocation) {
        String path = location.getPath();
        String prefix = fileLocation + "/";

        if (!path.startsWith(prefix)) {
            return "";
        }

        String relativePath = path.substring(prefix.length());
        int index = relativePath.lastIndexOf('/');

        return index < 0 ? "" : relativePath.substring(0, index);
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
            case "chat" -> picture = pictures.get(id);
            case "network" -> {
                picture = customPictures.get(id);
                if (picture == null) {
                    picture = networkPictures.get(id);
                }
                //如果本地没有这个图片，尝试向服务器请求
                if (picture == null && !requestedPictures.contains(id)) {
                    PacketDistributor.sendToServer(new C2SRequestPicturePacket(id));
                    requestedPictures.add(id);
                }
            }
        }

        return picture;
    }

    public @Nullable ChatPicture getCustomPicture(ResourceLocation location) {
        if(customSystemPictures.containsKey(location.toString())) {
            return customSystemPictures.get(location.toString());
        }
        else {
            Optional<Resource> optional = Minecraft.getInstance().getResourceManager().getResource(location);
            if(optional.isPresent()) {
                Resource resource = optional.get();
                try (InputStream inputStream = resource.open()) {
                    NativeImage image = NativeImage.read(inputStream);
                    ChatPicture picture = creatChatPicture(image, location, true);
                    customSystemPictures.put(location.toString(), picture);
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
        if (message == null) return false;
        return PICTURE_PATTERN.matcher(message).find();
    }

    private String extractIdPart(String message) {
        if (message == null) return null;
        Matcher m = PICTURE_PATTERN.matcher(message);
        return m.find() ? m.group(1) : null;
    }

    public String getPictureID(String message) {
        return extractIdPart(message);
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
        return new LinkedHashMap<>(pictures);
    }

    public Map<String, NetworkPicture> getNetworkData() {
        return new LinkedHashMap<>(networkData);
    }

    public Map<String, ChatPicture> getCustomPictures() {
        return new LinkedHashMap<>(customPictures);
    }

    public void clearRequested() {
        requestedPictures.clear();
    }

    public void removeRequested(String hash){
        requestedPictures.remove(hash);
    }

    public void handlePictureRequestResult(String hash, boolean success) {
        if (success) {
            MineChat.LOGGER.debug("[MineChat] Picture request succeeded: {}", hash);
        } else {
            MineChat.LOGGER.debug("[MineChat] Picture request failed: {}", hash);
        }
    }

    public Map<String, Map<String, ChatPicture>> getPictureGroups() {
        return new LinkedHashMap<>(pictureGroups);
    }

    private Path getCustomPictureConfigPath() {
        return Minecraft.getInstance()
                .gameDirectory
                .toPath()
                .resolve(CUSTOM_PICTURE_FOLDER)
                .resolve(CUSTOM_PICTURE_CONFIG);
    }

    private void printCustomPictureLoadProgress() {
        if (customPictureLoadTotal <= 0) {
            return;
        }

        int progress = customPictureLoadProgress * 100 / customPictureLoadTotal;

        // 只在每 10% 时打印一次
        int progressStep = (progress / 10) * 10;

        if (progressStep != lastCustomPictureProgress) {
            lastCustomPictureProgress = progressStep;

            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable(
                                "mine_chat.picture.custom_loading.progress",
                                customPictureLoadProgress,
                                customPictureLoadTotal
                        ),
                        true
                );
            }

            MineChat.LOGGER.info("[MineChat] Loading custom pictures: {}/{} ({}%)", customPictureLoadProgress, customPictureLoadTotal, progress);
        }
    }

    public void saveCustomPicturePaths() {
        Path configPath = getCustomPictureConfigPath();

        try {
            Files.createDirectories(configPath.getParent());

            JsonObject root = new JsonObject();
            JsonArray paths = new JsonArray();

            for (Path path : customPicturePaths) {
                paths.add(path.toAbsolutePath().normalize().toString());
            }

            root.add("paths", paths);

            Files.writeString(configPath, GSON.toJson(root), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        } catch (IOException e) {
            MineChat.LOGGER.error("[MineChat] Failed to save custom picture config", e);
        }
    }


    private void loadCustomPicturePath(Path path) {
        try {
            int oldQueueSize = customPictureLoadQueue.size();

            if (Files.isDirectory(path)) {

                try (Stream<Path> stream = Files.walk(path)) {
                    stream.filter(Files::isRegularFile)
                            .filter(this::isPictureFile)
                            .sorted(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER))
                            .forEach(customPictureLoadQueue::offer);
                }

            } else if (Files.isRegularFile(path) && isPictureFile(path)) {
                customPictureLoadQueue.offer(path);
            }

            int addedCount = customPictureLoadQueue.size() - oldQueueSize;

            if (addedCount > 0) {
                /*
                 * 如果当前没有正在进行的加载任务，
                 * 那么建立新的任务。
                 */
                if (!customPictureLoading) {
                    customPictureLoadTotal = 0;
                    customPictureLoadProgress = 0;
                    lastCustomPictureProgress = -1;
                }

                customPictureLoadTotal += addedCount;
                customPictureLoading = true;

                MineChat.LOGGER.info("[MineChat] Queued {} custom pictures, total: {}", addedCount, customPictureLoadTotal);
            }

        } catch (IOException e) {
            MineChat.LOGGER.error("[MineChat] Failed to scan custom picture path: {}", path, e);
        }
    }

    public boolean addCustomPicturePath(Path path) {
        if (path == null) {
            return false;
        }

        path = path.toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            return false;
        }

        if (Files.isRegularFile(path) && !isPictureFile(path)) {
            return false;
        }

        if (!Files.isDirectory(path) && !Files.isRegularFile(path)) {
            return false;
        }

        if (!customPicturePaths.contains(path)) {
            customPicturePaths.add(path);
            saveCustomPicturePaths();
            loadCustomPicturePath(path);

            return true;
        }

        return false;
    }

    public Set<Path> getCustomPicturePaths() {
        return new LinkedHashSet<>(customPicturePaths);
    }

    public boolean removeCustomPicturePath(Path path) {
        if (path == null) {
            return false;
        }

        path = path.toAbsolutePath().normalize();
        boolean removed = customPicturePaths.remove(path);
        if (!removed) {
            return false;
        }

        saveCustomPicturePaths();

        try (Stream<Path> walk = Files.walk(path)){
            walk.forEach(p->{
                Set<String> hash = customPathPictureHashes.get(p);
                if(hash != null) {
                    for (String hashKey : hash) {
                        customPictures.remove(hashKey);
                    }
                }
            });
        } catch (IOException ignored) {}

        MineChat.LOGGER.info("[MineChat] Removed custom picture path: {}", path);

        return true;
    }

    private boolean isPictureFile(Path path) {
        return detectPictureFormat(path) != PictureFormat.UNKNOWN;
    }
}