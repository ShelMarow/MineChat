package net.shelmarow.mine_chat.chat.playercache;

import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class PlayerCache {
    private boolean isOnline = true;
    private final UUID uuid;
    private final String name;
    private final ResourceLocation skinLocation;

    public PlayerCache(GameProfile profile, ResourceLocation skinLocation) {
        this.uuid = profile.getId();
        this.name = profile.getName();
        this.skinLocation = skinLocation;
    }

    public void updateOnlineStatus(boolean isOnline) {
        this.isOnline = isOnline;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public ResourceLocation getSkinLocation() {
        return skinLocation;
    }

    public boolean isOnline() {
        return isOnline;
    }
}
