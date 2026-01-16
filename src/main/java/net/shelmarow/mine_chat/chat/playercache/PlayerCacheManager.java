package net.shelmarow.mine_chat.chat.playercache;

import com.mojang.authlib.GameProfile;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.mine_chat.MineChat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MineChat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PlayerCacheManager {

    private static final Map<UUID, PlayerCache> PLAYER_CACHE_MAP = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onClientPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER || Minecraft.getInstance().player == null || Minecraft.getInstance().player != event.player) {
            return;
        }
        for (PlayerCache playerCache : PLAYER_CACHE_MAP.values()) {
            playerCache.updateOnlineStatus(checkPlayerOnline(playerCache.getProfile().getId()));
        }
    }

    public static void clearCache() {
        PLAYER_CACHE_MAP.clear();
    }

    public static PlayerInfo getPlayerInfo(UUID uuid) {Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            return mc.getConnection().getPlayerInfo(uuid);
        }
        return null;
    }

    public static void storePlayerCache(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(uuid);
            if (playerInfo != null) {
                GameProfile profile = playerInfo.getProfile();
                ResourceLocation skinLocation = mc.getSkinManager().getInsecureSkinLocation(profile);
                PLAYER_CACHE_MAP.put(uuid, new PlayerCache(profile, skinLocation));
            }
        }
    }

    public static PlayerCache getPlayerCache(String name, boolean store) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(name);
            if (playerInfo != null) {
                GameProfile profile = playerInfo.getProfile();
                ResourceLocation skinLocation = mc.getSkinManager().getInsecureSkinLocation(profile);
                if(store) {
                    PLAYER_CACHE_MAP.put(profile.getId(), new PlayerCache(profile, skinLocation));
                }
                return new PlayerCache(profile, skinLocation);
            }
        }
        return null;
    }

    public static List<PlayerCache> getPlayerCachesByName(String name) {
        List<PlayerCache> playerCaches = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().getOnlinePlayers().forEach(player -> {
                if (player.getProfile().getName().toLowerCase().startsWith(name.toLowerCase())) {
                    GameProfile profile = player.getProfile();
                    ResourceLocation skinLocation = mc.getSkinManager().getInsecureSkinLocation(profile);
                    playerCaches.add(new PlayerCache(profile, skinLocation));
                }
            });
        }
        return playerCaches;
    }

    public static PlayerCache getPlayerCache(UUID uuid) {
        return getPlayerCache(uuid, true);
    }

    public static PlayerCache getPlayerCache(UUID uuid, boolean shouldStore) {
        if(uuid.equals(Util.NIL_UUID)) {
            return null;
        }
        else if (!PLAYER_CACHE_MAP.containsKey(uuid) && shouldStore) {
            storePlayerCache(uuid);
        }
        return PLAYER_CACHE_MAP.get(uuid);
    }

    public static boolean checkPlayerOnline(UUID targetUUID) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            return mc.getConnection().getPlayerInfo(targetUUID) != null;
        }
        return false;
    }
}
