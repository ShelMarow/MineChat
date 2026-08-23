package net.shelmarow.mine_chat.chat.storage;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.npc.NPCDialog;
import net.shelmarow.mine_chat.chat.npc.NPCDialogManager;
import net.shelmarow.mine_chat.chat.npc.NPCDialogRegister;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;

public class ServerDataStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String MOD_FOLDER = "mine_chat";
    private static final String DIALOG_DATA_FOLDER = "dialog_data";

    private static Path getRootPath(MinecraftServer server) {
        if (server == null) {
            return null;
        }

        return server.getWorldPath(LevelResource.ROOT).resolve(MOD_FOLDER).resolve(DIALOG_DATA_FOLDER);
    }

    private static Path getPlayerPath(ServerPlayer player) {
        if (player == null) {
            return null;
        }

        Path root = getRootPath(player.server);
        return root.resolve(player.getUUID() + ".json");
    }

    public static void saveNPCProgress(ServerPlayer player) {
        if (player == null) {
            return;
        }

        try {
            Path root = getRootPath(player.server);
            Files.createDirectories(root);
            Path savePath = getPlayerPath(player);

            if (savePath == null) {
                return;
            }

            UUID playerUUID = player.getUUID();
            Map<UUID, Map<UUID, Deque<NPCDialog>>> allQuests = NPCDialogManager.getInstance().getQuests();
            Map<UUID, Deque<NPCDialog>> playerQuests = allQuests.get(playerUUID);

            JsonObject rootJson = new JsonObject();
            JsonArray progress = new JsonArray();

            if (playerQuests != null) {
                for (Deque<NPCDialog> dialogs : playerQuests.values()) {
                    if (dialogs == null || dialogs.isEmpty()) {
                        continue;
                    }
                    for (NPCDialog dialog : dialogs) {
                        if (dialog == null) {
                            continue;
                        }
                        progress.add(dialog.toJson());
                    }
                }
            }

            rootJson.add("progress", progress);
            Files.writeString(savePath, GSON.toJson(rootJson), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (Exception e) {
            MineChat.LOGGER.error("Failed to save NPC progress for player {}", player.getUUID(), e);
        }
    }


    public static void loadNPCProgress(ServerPlayer player) {
        if (player == null) {
            return;
        }

        try {
            Path savePath = getPlayerPath(player);

            if (savePath == null || !Files.exists(savePath)) {
                return;
            }

            String content = Files.readString(savePath);

            if (content.isBlank()) {
                return;
            }

            JsonElement parsed = JsonParser.parseString(content);

            if (!parsed.isJsonObject()) {
                MineChat.LOGGER.warn("Invalid NPC progress file: {}", savePath);
                return;
            }

            JsonObject rootJson = parsed.getAsJsonObject();

            JsonArray progress = rootJson.getAsJsonArray("progress");

            if (progress == null) {
                return;
            }

            for (JsonElement element : progress) {

                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject data = element.getAsJsonObject();

                JsonElement dialogIdElement = data.get("dialogID");

                if (dialogIdElement == null || dialogIdElement.isJsonNull()) {
                    continue;
                }

                String dialogId = dialogIdElement.getAsString();

                NPCDialog dialog = NPCDialogRegister.getNPCDialog(dialogId);

                if (dialog == null) {
                    MineChat.LOGGER.warn("Cannot find NPC dialog with ID: {}", dialogId);
                    continue;
                }

                dialog.fromJson(data);
                UUID npcUUID = dialog.getChatSender().getUuid();

                NPCDialogManager.getInstance().addNPCDialogQuest(npcUUID, player, dialog, false);
            }
        }
        catch (JsonParseException e) {
            MineChat.LOGGER.error("Invalid JSON in NPC progress file for player {}", player.getUUID(), e);
        }
        catch (Exception e) {
            MineChat.LOGGER.error("Failed to load NPC progress for player {}", player.getUUID(), e);
        }
    }
}