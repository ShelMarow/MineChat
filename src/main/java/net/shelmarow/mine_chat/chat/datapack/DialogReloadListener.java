package net.shelmarow.mine_chat.chat.datapack;

import com.google.gson.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.shelmarow.mine_chat.chat.npc.NPCDialog;
import net.shelmarow.mine_chat.chat.npc.NPCDialogRegister;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.NPCSenderManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DialogReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "npc_dialog";
    private static final List<NPCDialogProvider> NPC_DIALOG_PROVIDERS = new ArrayList<>();

    public DialogReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected @NotNull Map<ResourceLocation, JsonElement> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        NPC_DIALOG_PROVIDERS.clear();
        NPCDialogRegister.clearDatapackDialogs();
        return super.prepare(resourceManager, profiler);
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> jsonMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {

            //读取json内容
            JsonObject json = entry.getValue().getAsJsonObject();

            ResourceLocation rl = entry.getKey();
            String pathString = rl.getPath();
            ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), pathString);

            UUID sender = UUID.fromString(json.get("sender").getAsString());
            boolean openScreen = json.get("openScreen").getAsBoolean();

            List<NPCDialogActionProvider> actions = new ArrayList<>();
            JsonArray array = json.getAsJsonArray("dialogs");
            for(JsonElement jsonElement : array){
                JsonObject object = jsonElement.getAsJsonObject();

                String message = object.get("message").getAsString();
                String option = object.has("option") ? object.get("option").getAsString() : "";

                List<String> commandList = new ArrayList<>();
                if(object.has("commands")){
                    JsonArray commands = object.get("commands").getAsJsonArray();
                    for(JsonElement jsonElement1 : commands){
                        JsonObject command = jsonElement1.getAsJsonObject();
                        String commandString = command.get("command").getAsString();
                        commandList.add(commandString);
                    }
                }
                actions.add(new NPCDialogActionProvider(message, option, commandList));
            }

            registerDialog(sender, registryName, openScreen, actions);

            NPCDialogProvider dialogProvider = new NPCDialogProvider(registryName.toString(), sender, openScreen, actions);
            NPC_DIALOG_PROVIDERS.add(dialogProvider);

        }
    }

    private static void registerDialog(UUID sender, ResourceLocation registryName, boolean openScreen, List<NPCDialogActionProvider> actions) {
        ChatSender chatSender = NPCSenderManager.getInstance().getNpcData(sender);
        if(chatSender != null){
            NPCDialog.Builder builder = new NPCDialog.Builder(registryName.toString(), chatSender, openScreen);
            for (NPCDialogActionProvider actionProvider : actions) {
                builder.sendMessage(Component.translatable(actionProvider.getMessage()), actionProvider.option.isEmpty() ? List.of() : List.of(actionProvider.option), p->{
                    if(p instanceof ServerPlayer serverPlayer){
                        MinecraftServer server = serverPlayer.getServer();
                        if (server != null) {
                            CommandSourceStack source = serverPlayer.createCommandSourceStack().withPermission(2).withSuppressedOutput();

                            for (String command : actionProvider.commands){
                                server.getCommands().performPrefixedCommand(source, command);
                            }
                        }
                    }
                });
            }
            builder.buildDatapack();
        }
    }


    public static List<NPCDialogProvider> getNpcDialogProviders(){
        return NPC_DIALOG_PROVIDERS;
    }




    public static class NPCDialogProvider {
        private String id;
        private UUID sender;
        private boolean openScreen;
        private List<NPCDialogActionProvider> actions;

        public NPCDialogProvider() {
        }

        public NPCDialogProvider(String id, UUID sender, boolean openScreen, List<NPCDialogActionProvider> actions) {
            this.actions = actions;
            this.openScreen = openScreen;
            this.sender = sender;
            this.id = id;
        }

        public @NotNull CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id);
            tag.putString("sender", sender.toString());
            tag.putBoolean("openScreen", openScreen);
            ListTag listTag = new ListTag();
            for(NPCDialogActionProvider actionProvider : actions){
                listTag.add(actionProvider.serializeNBT());
            }
            tag.put("dialogs", listTag);
            return tag;
        }

        public void deserializeNBT(@NotNull CompoundTag nbt) {
            this.id = nbt.getString("id");
            this.sender = UUID.fromString(nbt.getString("sender"));
            this.openScreen = nbt.getBoolean("openScreen");
            List<NPCDialogActionProvider> actions = new ArrayList<>();
            ListTag listTag =  nbt.getList("dialogs", Tag.TAG_COMPOUND);
            for(int i = 0; i < listTag.size(); i++){
                CompoundTag compoundTag = listTag.getCompound(i);
                NPCDialogActionProvider actionProvider = new NPCDialogActionProvider();
                actionProvider.deserializeNBT(compoundTag);
                actions.add(actionProvider);
            }
            this.actions = actions;
        }

        public String getId() {
            return id;
        }

        public UUID getSender() {
            return sender;
        }

        public List<NPCDialogActionProvider> getActions() {
            return actions;
        }

        public boolean isOpenScreen() {
            return openScreen;
        }

        public void setOpenScreen(boolean openScreen) {
            this.openScreen = openScreen;
        }
    }

    public static class NPCDialogActionProvider{
        private String message;
        private String option;
        private List<String> commands;

        public NPCDialogActionProvider() {}

        public NPCDialogActionProvider(String message, String option, List<String> commands) {
            this.message = message;
            this.option = option;
            this.commands = commands;
        }



        public @NotNull CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("message", message);
            tag.putString("option", option);
            ListTag listTag = new ListTag();
            for(String command : commands){
                CompoundTag commandTag = new CompoundTag();
                commandTag.putString("command", command);
                listTag.add(commandTag);
            }
            tag.put("commands", listTag);
            return tag;
        }

        public void deserializeNBT(@NotNull CompoundTag nbt) {
            this.message = nbt.getString("message");
            this.option = nbt.getString("option");
            List<String> commands = new ArrayList<>();
            ListTag listTag = nbt.getList("commands", Tag.TAG_COMPOUND);
            for(int i = 0; i < listTag.size(); i++){
                CompoundTag commandTag = listTag.getCompound(i);
                commands.add(commandTag.getString("command"));
            }
            this.commands = commands;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getOption() {
            return option;
        }

        public void setOption(String option) {
            this.option = option;
        }

        public List<String> getCommands() {
            return commands;
        }

        public void setCommands(List<String> commands) {
            this.commands = commands;
        }
    }
}
