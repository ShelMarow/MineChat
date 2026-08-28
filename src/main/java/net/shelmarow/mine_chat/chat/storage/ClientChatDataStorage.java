package net.shelmarow.mine_chat.chat.storage;

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.MineChatManager;
import net.shelmarow.mine_chat.chat.message.AnimationMessage;
import net.shelmarow.mine_chat.chat.message.chat_enum.MessageType;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.NPCSenderManager;
import net.shelmarow.mine_chat.chat.sender.SenderType;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;


@OnlyIn(Dist.CLIENT)
public class ClientChatDataStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();


    private static Path getRootPath() {
        Minecraft mc = Minecraft.getInstance();
        Path root = mc.gameDirectory.toPath().resolve("mine_chat");

        if (mc.getSingleplayerServer() != null) {
            String worldName = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).getParent().getFileName().toString();
            return root.resolve("singleplayer").resolve(sanitize(worldName));
        }

        if (mc.getCurrentServer() != null) {
            return root.resolve("servers").resolve(sanitize(mc.getCurrentServer().ip));
        }
        return null;
    }


    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static void saveDM() {
        savePlayerDM();
        saveNPCDM();
    }

    private static JsonArray serializeMessages(ArrayDeque<AnimationMessage> messages, HolderLookup.Provider provider, boolean npc) {
        JsonArray array = new JsonArray();
        for (AnimationMessage msg : messages) {
            if (msg.getSender() == null) continue;

            JsonObject json = new JsonObject();
            json.addProperty("senderUuid", msg.getSender().getUuid().toString());
            json.addProperty("senderName", msg.getSender().getName());
            if (npc) {
                json.addProperty("senderType", msg.getSender().getSenderType().name());
            }

            json.addProperty("timestamp", msg.getTimestamp());
            json.addProperty("nameLength", msg.getNameLength());
            json.addProperty("messageType", msg.getMessageType().name());
            json.addProperty("text", Component.Serializer.toJson(msg.getMessage(), provider));
            array.add(json);
        }
        return array;
    }

    public static void savePlayerDM() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Path root = getRootPath();
            if (root == null) return;
            HolderLookup.Provider provider = mc.level.registryAccess();
            Path folder = root.resolve("dm").resolve("players");
            Files.createDirectories(folder);
            for (Map.Entry<UUID, Pair<ArrayDeque<AnimationMessage>, Long>> entry : MineChatManager.getDMMap().entrySet()) {
                JsonObject json = new JsonObject();
                json.add("messages", serializeMessages(entry.getValue().getFirst(), provider, false));
                Files.writeString(folder.resolve(entry.getKey() + ".json"), GSON.toJson(json), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (Exception e) {
            MineChat.LOGGER.error("Failed to save player DM", e);
        }

    }


    public static void saveNPCDM() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Path root = getRootPath();
            if (root == null) return;
            HolderLookup.Provider provider = mc.level.registryAccess();
            Path folder = root.resolve("dm").resolve("npcs");
            Files.createDirectories(folder);
            for (Map.Entry<UUID, Pair<ArrayDeque<AnimationMessage>, Long>> entry : MineChatManager.getNPCMap().entrySet()) {
                JsonObject json = new JsonObject();
                json.add("messages", serializeMessages(entry.getValue().getFirst(), provider, true));
                Files.writeString(folder.resolve(entry.getKey() + ".json"), GSON.toJson(json), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (Exception e) {
            MineChat.LOGGER.error("Failed to save NPC DM", e);
        }

    }


    public static void loadDM() {
        loadPlayerDM();
        loadNPCDM();
    }


    public static void loadPlayerDM() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Path root = getRootPath();
            if (root == null) return;

            Path folder = root.resolve("dm").resolve("players");
            if (!Files.exists(folder)) return;

            HolderLookup.Provider provider = mc.level.registryAccess();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.json")){
                for (Path path : stream) {
                    UUID target = UUID.fromString(path.getFileName().toString().replace(".json", ""));
                    JsonObject rootJson = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    ArrayDeque<AnimationMessage> messages = new ArrayDeque<>();
                    JsonArray array = rootJson.getAsJsonArray("messages");
                    if (array != null) {
                        for (JsonElement element : array) {
                            JsonObject json = element.getAsJsonObject();
                            UUID sender = UUID.fromString(json.get("senderUuid").getAsString());
                            String senderName = json.get("senderName").getAsString();
                            ChatSender chatSender = new ChatSender(sender, senderName, null, SenderType.PLAYER);
                            AnimationMessage msg = new AnimationMessage(
                                    chatSender, json.get("timestamp").getAsLong(), json.get("nameLength").getAsInt(),
                                    MessageType.valueOf(json.get("messageType").getAsString()),
                                    5,0,0,
                                    Component.Serializer.fromJson(json.get("text").getAsString(), provider)
                            );
                            msg.setRemainTime(msg.getMaxRemainTime());
                            messages.addLast(msg);
                        }
                    }
                    MineChatManager.getDMMap().put(target, Pair.of(messages, 0L));
                }
            } catch (Exception e) {
                MineChat.LOGGER.error("Failed load player chat file", e);
            }
        } catch (Exception e) {
            MineChat.LOGGER.error("Failed load player DM", e);
        }

    }

    public static void loadNPCDM() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Path root = getRootPath();
            if (root == null) return;

            Path folder = root.resolve("dm").resolve("npcs");
            if (!Files.exists(folder)) return;

            HolderLookup.Provider provider = mc.level.registryAccess();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.json")){
                for (Path path : stream) {
                    UUID target = UUID.fromString(path.getFileName().toString().replace(".json", ""));
                    JsonObject rootJson = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    ArrayDeque<AnimationMessage> messages = new ArrayDeque<>();
                    JsonArray array = rootJson.getAsJsonArray("messages");

                    if (array != null) {
                        for (JsonElement element : array) {
                            JsonObject json = element.getAsJsonObject();
                            UUID sender = UUID.fromString(json.get("senderUuid").getAsString());
                            SenderType senderType = SenderType.valueOf(json.get("senderType").getAsString());

                            ChatSender senderObj = null;

                            if (senderType == SenderType.NPC) {
                                senderObj = NPCSenderManager.getInstance().getNpcData(sender);
                            }

                            if (senderObj == null) {
                                senderObj = new ChatSender(sender, null, null, senderType);
                            }

                            AnimationMessage msg = new AnimationMessage(
                                    senderObj, json.get("timestamp").getAsLong(), json.get("nameLength").getAsInt(),
                                    MessageType.valueOf(json.get("messageType").getAsString()),
                                    5,0,0,
                                    Component.Serializer.fromJson(json.get("text").getAsString(), provider)
                            );
                            msg.setRemainTime(msg.getMaxRemainTime());
                            messages.addLast(msg);
                        }
                    }
                    MineChatManager.getNPCMap().put(target, Pair.of(messages, 0L));
                }
            } catch (Exception e) {
                MineChat.LOGGER.error("Failed load NPC chat file", e);
            }
        } catch (Exception e) {
            MineChat.LOGGER.error("Failed load NPC DM", e);
        }

    }



}