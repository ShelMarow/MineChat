package net.shelmarow.mine_chat.chat.playercache;

import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerCache {
    private boolean isOnline = true;
    private final GameProfile profile;
    private final ResourceLocation skinLocation;

    public PlayerCache(GameProfile profile, ResourceLocation skinLocation) {
        this.profile = profile;
        this.skinLocation = skinLocation;
    }

    public void updateOnlineStatus(boolean isOnline) {
        this.isOnline = isOnline;
    }

    public GameProfile getProfile() {
        return profile;
    }

    public ResourceLocation getSkinLocation() {
        return skinLocation;
    }

    public boolean isOnline() {
        return isOnline;
    }
}
