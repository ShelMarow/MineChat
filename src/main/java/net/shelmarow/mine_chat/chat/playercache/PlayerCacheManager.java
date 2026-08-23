package net.shelmarow.mine_chat.chat.playercache;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.mine_chat.MineChat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MineChat.MOD_ID, value = Dist.CLIENT)
public class PlayerCacheManager {

    private static final Map<UUID, PlayerCache> PLAYER_CACHE_MAP = new ConcurrentHashMap<>();
    private static final Map<UUID, String> LOCAL_PLAYER_CACHE_MAP = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CACHE_FOLDER = "mine_chat/player_cache";
    private static final String CACHE_FILE = "players.json";

    @SubscribeEvent
    public static void onClientPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        loadLocalPlayerCache();
    }

    @SubscribeEvent
    public static void onClientPlayerTick(TickEvent.PlayerTickEvent event) {
        if(event.side == LogicalSide.CLIENT && event.phase == TickEvent.Phase.START) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.player != event.player) return;
            for (PlayerCache playerCache : PLAYER_CACHE_MAP.values()) {
                playerCache.updateOnlineStatus(checkPlayerOnline(playerCache.getUuid()));
            }
        }
    }

    private static Path getCacheFolder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(CACHE_FOLDER);
    }

    private static Path getCacheFile() {
        return getCacheFolder().resolve(CACHE_FILE);
    }

    /** 从本地加载玩家名称缓存 (UUID -> 名称) */
    private static void loadLocalPlayerCache() {
        try {
            Path file = getCacheFile();
            if (!Files.exists(file)) return;
            String content = Files.readString(file);
            if (content.isBlank()) return;
            JsonElement element = JsonParser.parseString(content);
            if (!element.isJsonObject()) {
                MineChat.LOGGER.warn("Invalid player cache file: {}", file);
                return;
            }
            JsonObject root = element.getAsJsonObject();
            JsonObject players = root.getAsJsonObject("players");
            if (players == null) return;
            LOCAL_PLAYER_CACHE_MAP.clear();
            for (Map.Entry<String, JsonElement> entry : players.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    String name = entry.getValue().getAsString();
                    if (name == null || name.isBlank()) continue;
                    LOCAL_PLAYER_CACHE_MAP.put(uuid, name);
                } catch (Exception e) {
                    MineChat.LOGGER.warn("Invalid player cache entry: {}", entry.getKey());
                }
            }
            MineChat.LOGGER.debug("Loaded {} local player caches", LOCAL_PLAYER_CACHE_MAP.size());
        } catch (Exception e) {
            MineChat.LOGGER.error("Failed to load local player cache", e);
        }
    }

    /** 保存本地玩家名称缓存 */
    private static void saveLocalPlayerCache() {
        try {
            Files.createDirectories(getCacheFolder());
            JsonObject root = new JsonObject();
            JsonObject players = new JsonObject();
            for (Map.Entry<UUID, String> entry : LOCAL_PLAYER_CACHE_MAP.entrySet()) {
                players.addProperty(entry.getKey().toString(), entry.getValue());
            }
            root.add("players", players);
            Files.writeString(getCacheFile(), GSON.toJson(root), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (Exception e) {
            MineChat.LOGGER.error("Failed to save local player cache", e);
        }
    }

    /** 存储本地玩家，仅在首次发现或名称变化时写入磁盘 */
    private static void storeLocalPlayer(GameProfile profile) {
        if (profile == null || profile.getId() == null || profile.getName() == null) return;
        UUID uuid = profile.getId();
        String name = profile.getName();
        String oldName = LOCAL_PLAYER_CACHE_MAP.put(uuid, name);
        if (!name.equals(oldName)) {
            saveLocalPlayerCache();
        }
    }

    public static void clearCache() {
        PLAYER_CACHE_MAP.clear();
    }

    @Nullable
    public static PlayerInfo getPlayerInfo(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            return mc.getConnection().getPlayerInfo(uuid);
        }
        return null;
    }

    private static PlayerCache createPlayerCache(PlayerInfo playerInfo) {
        GameProfile profile = playerInfo.getProfile();
        ResourceLocation skinLocation = playerInfo.getSkinLocation();
        storeLocalPlayer(profile);
        return new PlayerCache(profile, skinLocation);
    }

    /** 缓存在线玩家 */
    public static void storePlayerCache(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(uuid);
        if (playerInfo == null) return;
        PLAYER_CACHE_MAP.put(uuid, createPlayerCache(playerInfo));
    }

    @Nullable
    public static PlayerCache getPlayerCache(String name, boolean store) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.getConnection() != null) {
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(name);
            if (playerInfo != null) {
                PlayerCache cache = createPlayerCache(playerInfo);
                if (store) {
                    PLAYER_CACHE_MAP.put(cache.getUuid(), cache);
                }
                return cache;
            }
        }

        for (Map.Entry<UUID, String> entry : LOCAL_PLAYER_CACHE_MAP.entrySet()) {
            if (!entry.getValue().equalsIgnoreCase(name)) continue;
            return loadOfflinePlayer(entry.getKey(), entry.getValue());
        }
        return null;
    }


    public static List<PlayerCache> getPlayerCachesByName(String name) {
        List<PlayerCache> playerCaches = new ArrayList<>();
        String lowerName = name.toLowerCase();
        Minecraft mc = Minecraft.getInstance();

        if (mc.getConnection() != null) {
            mc.getConnection().getOnlinePlayers().forEach(player -> {
                String playerName = player.getProfile().getName();
                if (!playerName.toLowerCase().startsWith(lowerName)) return;
                PlayerCache cache = createPlayerCache(player);
                playerCaches.add(cache);
                PLAYER_CACHE_MAP.put(cache.getUuid(), cache);
            });
        }

        for (Map.Entry<UUID, String> entry : LOCAL_PLAYER_CACHE_MAP.entrySet()) {
            String playerName = entry.getValue();
            if (!playerName.toLowerCase().startsWith(lowerName)) continue;
            boolean exists = playerCaches.stream().anyMatch(cache -> cache.getUuid().equals(entry.getKey()));
            if (exists) continue;
            PlayerCache cache = loadOfflinePlayer(entry.getKey(), playerName);
            playerCaches.add(cache);
        }
        return playerCaches;
    }

    @Nullable
    public static PlayerCache getPlayerCache(UUID uuid) {
        return getPlayerCache(uuid, true);
    }

    @Nullable
    public static PlayerCache getPlayerCache(UUID uuid, boolean shouldStore) {
        if (uuid == null || uuid.equals(Util.NIL_UUID)) return null;

        PlayerCache cache;
        if (shouldStore) {
            storePlayerCache(uuid);
            cache = PLAYER_CACHE_MAP.get(uuid);
            if (cache != null) {
                return cache;
            }
        }

        String name = LOCAL_PLAYER_CACHE_MAP.get(uuid);
        if (name == null) return null;
        return loadOfflinePlayer(uuid, name);
    }

    private static @NotNull PlayerCache loadOfflinePlayer(UUID uuid, String name) {
        Minecraft mc = Minecraft.getInstance();
        GameProfile profile = new GameProfile(uuid, name);

        ResourceLocation location = mc.getSkinManager().getInsecureSkinLocation(profile);
        PlayerCache cache = new PlayerCache(profile, location);
        cache.updateOnlineStatus(false);
        PLAYER_CACHE_MAP.put(uuid, cache);

        return createDefaultOfflinePlayer(profile);
    }

    private static PlayerCache createDefaultOfflinePlayer(GameProfile profile) {
        ResourceLocation defaultSkin = DefaultPlayerSkin.getDefaultSkin(profile.getId());
        PlayerCache cache = new PlayerCache(profile, defaultSkin);
        cache.updateOnlineStatus(false);
        PLAYER_CACHE_MAP.put(profile.getId(), cache);
        return cache;
    }

    public static boolean checkPlayerOnline(UUID targetUUID) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            return mc.getConnection().getPlayerInfo(targetUUID) != null;
        }
        return false;
    }
}