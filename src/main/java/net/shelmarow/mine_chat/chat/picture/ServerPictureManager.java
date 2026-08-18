package net.shelmarow.mine_chat.chat.picture;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.picture.data.NetworkPicture;
import net.shelmarow.mine_chat.network.PicturePacketManager;
import net.shelmarow.mine_chat.network.packet.server.S2CPictureRequestResultPacket;
import net.shelmarow.mine_chat.network.packet.server.S2CSendPicturePacket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

public class ServerPictureManager {

    private static final ServerPictureManager INSTANCE = new ServerPictureManager();

    /**
     * 每次等待的时间。
     */
    private static final long REQUEST_TIMEOUT = 2000L;

    /**
     * 图片不存在时最多重新检查次数。
     */
    private static final int MAX_RETRY_COUNT = 5;

    private final Path pictureFolder = Path.of("mine_chat", "pictures", "network");
    private final Map<String, NetworkPicture> networkPictures = new HashMap<>();
    private final Map<UUID, Map<String, PictureRequest>> requestList = new HashMap<>();

    private ServerPictureManager() {
        loadPictures();
    }

    public static ServerPictureManager getInstance() {
        return INSTANCE;
    }

    /**
     * 检查等待中的图片请求。
     * <p>
     * 流程：
     * <p>
     * 1. 等待 REQUEST_TIMEOUT
     * 2. 检查服务端是否已经拥有图片
     * 3. 如果存在，则发送并移除请求
     * 4. 如果不存在，则进行一次重试
     * 5. 超过 MAX_RETRY_COUNT 后放弃请求
     */
    public void tick() {
        if (requestList.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, Map<String, PictureRequest>>> playerIterator = requestList.entrySet().iterator();

        while (playerIterator.hasNext()) {
            Map.Entry<UUID, Map<String, PictureRequest>> playerEntry = playerIterator.next();

            UUID player = playerEntry.getKey();
            Map<String, PictureRequest> requests = playerEntry.getValue();

            Iterator<Map.Entry<String, PictureRequest>> requestIterator = requests.entrySet().iterator();

            while (requestIterator.hasNext()) {
                Map.Entry<String, PictureRequest> requestEntry = requestIterator.next();

                String hash = requestEntry.getKey();
                PictureRequest request = requestEntry.getValue();

                // 尚未达到超时时间
                if (now - request.requestTime < REQUEST_TIMEOUT) {
                    continue;
                }

                request.retryCount++;

                boolean finished = false;
                if (hasPicture(hash)) {
                    if (sendToClient(hash, player)) {
                        finished = true;
                        sendRequestResult(playerEntry.getKey(), requestEntry.getKey(), true);
                        MineChat.LOGGER.debug("[MineChat] Retried picture request successfully: player={}, hash={}, retry={}/{}", player, hash, request.retryCount, MAX_RETRY_COUNT);
                    }
                }

                if (!finished && request.retryCount >= MAX_RETRY_COUNT) {
                    finished = true;
                    sendRequestResult(playerEntry.getKey(), requestEntry.getKey(), false);
                    MineChat.LOGGER.debug("[MineChat] Picture request failed after {} retries: player={}, hash={}", MAX_RETRY_COUNT, player, hash);
                }

                if (finished) {
                    requestIterator.remove();
                } else {
                    request.requestTime = now;
                }
            }

            if (requests.isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    public void storeNetworkPicture(String hash, NetworkPicture networkPicture) {
        networkPictures.put(hash, networkPicture);

        savePicture(hash, networkPicture);

        // 图片到达服务器后，立即处理等待中的请求
        sendPendingRequests(hash);
    }


    private void sendPendingRequests(String hash) {
        Iterator<Map.Entry<UUID, Map<String, PictureRequest>>> iterator = requestList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Map<String, PictureRequest>> entry = iterator.next();
            UUID player = entry.getKey();
            Map<String, PictureRequest> requests = entry.getValue();
            if (requests.remove(hash) != null) {
                if (sendToClient(hash, player)) {
                    MineChat.LOGGER.debug("[MineChat] Sent pending picture: player={}, hash={}", player, hash);
                }
            }
            if (requests.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public void requestPictureToClient(String hash, UUID player) {
        // 服务端已经存在，直接发送
        if (hasPicture(hash)) {
            if (sendToClient(hash, player)) {
                return;
            }
        }
        // 服务端不存在，加入等待队列
        Map<String, PictureRequest> requests = requestList.computeIfAbsent(player, k -> new HashMap<>());
        requests.computeIfAbsent(hash, k -> new PictureRequest(System.currentTimeMillis()));
    }

    public void confirmReceived(String hash, UUID uuid) {
        // 暂时不处理
    }

    private void sendRequestResult(UUID player, String hash, boolean success) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player);
            if (serverPlayer != null) {
                PacketDistributor.sendToPlayer(serverPlayer, new S2CPictureRequestResultPacket(hash, success));
            }
        }
    }

    public boolean sendToClient(String hash, UUID player) {
        NetworkPicture networkPicture = networkPictures.get(hash);
        if (networkPicture != null) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player);
                if (serverPlayer != null) {
                    List<S2CSendPicturePacket> packets = PicturePacketManager.splitPictureS2C(networkPicture);
                    for (S2CSendPicturePacket packet : packets) {
                        PacketDistributor.sendToPlayer(serverPlayer, packet);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void savePicture(String hash, NetworkPicture networkPicture) {
        try {
            Files.createDirectories(pictureFolder);
            String extension = networkPicture.isGif() ? ".gif" : ".png";
            Path file = pictureFolder.resolve(hash + extension);
            byte[] data = networkPicture.getImageData();
            Files.write(file, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            MineChat.LOGGER.error("[MineChat] Failed to save network picture: {}", hash, e);
        }
    }

    private void loadPictures() {
        if (!Files.isDirectory(pictureFolder)) {
            return;
        }
        try (Stream<Path> pathStream = Files.list(pictureFolder)) {
            pathStream.filter(Files::isRegularFile).filter(this::isPictureFile).forEach(this::loadPicture);
        } catch (IOException e) {
            MineChat.LOGGER.error("[MineChat] Failed to load network pictures", e);
        }
        MineChat.LOGGER.info("[MineChat] Loaded network pictures: {}", networkPictures.size());
    }

    private boolean isPictureFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".png") || fileName.endsWith(".gif");
    }

    private void loadPicture(Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return;
        }

        String hash = fileName.substring(0, dot);
        String extension = fileName.substring(dot).toLowerCase();
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (extension.equals(".png")) {
                NetworkPicture picture = new NetworkPicture(hash, bytes, false);
                networkPictures.put(hash, picture);
            } else if (extension.equals(".gif")) {
                NetworkPicture picture = new NetworkPicture(hash, bytes, true);
                networkPictures.put(hash, picture);
            }
        } catch (Exception e) {
            MineChat.LOGGER.error("[MineChat] Failed to load network picture: {}", path, e);
        }
    }

    public boolean hasPicture(String hash) {
        return networkPictures.containsKey(hash);
    }

    private static class PictureRequest {
        private long requestTime;
        private int retryCount;
        private PictureRequest(long requestTime) {
            this.requestTime = requestTime;
            this.retryCount = 0;
        }
    }
}