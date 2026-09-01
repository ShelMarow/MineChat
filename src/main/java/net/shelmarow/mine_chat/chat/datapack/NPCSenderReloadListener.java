package net.shelmarow.mine_chat.chat.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.SenderType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NPCSenderReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "npc_sender";
    private static final List<ChatSender> SENDERS = new ArrayList<>();

    public NPCSenderReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected @NotNull Map<ResourceLocation, JsonElement> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        SENDERS.clear();
        return super.prepare(resourceManager, profiler);
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> jsonMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            JsonElement element = entry.getValue();
            JsonObject json = element.getAsJsonObject();
            loadNPCSender(entry.getKey(), json);
        }
    }

    private static void loadNPCSender(ResourceLocation id, JsonObject json) {

        UUID uuid = UUID.fromString(json.get("uuid").getAsString());

        String name = null;

        if (json.has("name")) {
            name = json.get("name").getAsString();
        }

        ResourceLocation head = null;
        if (json.has("head")) {
            String headString = json.get("head").getAsString();
            if (!headString.isEmpty()) {
                head = ResourceLocation.tryParse(headString);
            }
        }

        ChatSender sender = new ChatSender(uuid, name, head, SenderType.NPC);

        if (json.has("customHead")) {
            sender.setCustomHead(json.get("customHead").getAsBoolean());
        }

        SENDERS.add(sender);
    }

    public static List<ChatSender> getSenders(){
        return SENDERS;
    }
}
