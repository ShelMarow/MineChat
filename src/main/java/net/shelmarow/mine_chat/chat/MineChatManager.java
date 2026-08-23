package net.shelmarow.mine_chat.chat;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.message.AnimationMessage;
import net.shelmarow.mine_chat.chat.message.chat_enum.AnimationStatus;
import net.shelmarow.mine_chat.chat.message.chat_enum.MessageType;
import net.shelmarow.mine_chat.chat.npc.ClientDialogProcessHandler;
import net.shelmarow.mine_chat.chat.picture.ClientPictureManager;
import net.shelmarow.mine_chat.chat.playercache.PlayerCache;
import net.shelmarow.mine_chat.chat.playercache.PlayerCacheManager;
import net.shelmarow.mine_chat.chat.screen.MineChatDMScreen;
import net.shelmarow.mine_chat.chat.screen.MineChatGlobeScreen;
import net.shelmarow.mine_chat.chat.screen.MineChatTeamScreen;
import net.shelmarow.mine_chat.chat.sender.ChatSender;
import net.shelmarow.mine_chat.chat.sender.NPCSenderManager;
import net.shelmarow.mine_chat.chat.sender.SenderType;
import net.shelmarow.mine_chat.chat.sound.MineChatSounds;
import net.shelmarow.mine_chat.chat.storage.ClientChatDataStorage;
import net.shelmarow.mine_chat.config.MineChatClientConfig;
import net.shelmarow.mine_chat.network.packet.client.C2SServerInstallTestPacket;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@EventBusSubscriber(modid = MineChat.MOD_ID, value = Dist.CLIENT)
public class MineChatManager {
    //最大可记录的消息条数
    private static final int MAX_SIZE = 200;

    //全局消息
    private static final ArrayDeque<AnimationMessage> CHAT_GLOBE = new ArrayDeque<>();
    private static final ArrayDeque<AnimationMessage> CHAT_GLOBE_DISPLAY = new ArrayDeque<>();
    //未检查的全局@消息
    private static boolean uncheckedPingMessage = false;

    //私聊消息
    private static final Map<UUID, Pair<ArrayDeque<AnimationMessage>, Long>> CHAT_DM_MAP = new HashMap<>();
    //未确认的私聊消息名单
    private static final Set<UUID> CHAT_DM_UNCHECKED = new HashSet<>();
    //NPC的私聊消息
    private static final Map<UUID, Pair<ArrayDeque<AnimationMessage>, Long>> NPC_DM_MAP = new HashMap<>();
    //未确认的NPC私聊消息名单
    private static final Set<UUID> NPC_DM_UNCHECKED = new HashSet<>();

    //队伍消息
    private static final ArrayDeque<AnimationMessage> CHAT_TEAM = new ArrayDeque<>();
    //是否确认过队伍消息
    private static boolean teamChatChecked = true;

    //是否显示消息图标（在一定时间内没有最新消息时，小图标消失）
    private static final long ICON_TIME = 100;
    private static final long ICON_FADE_TIME = 80;
    private static long showIconTime = 0;

    //起否启用彩蛋旋转
    private static boolean shouldRotation = false;

    private static int autoSaveTime = 0;
    private static boolean isDMLoaded = false;

    //服务端是否安装了MineChat
    private static boolean isServerInstalled = false;

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if(MineChatClientConfig.ENABLE_NETWORK_PICTURE.get()){
            try{
                PacketDistributor.sendToServer(new C2SServerInstallTestPacket());
            } catch (Exception e){
                setServerInstalled(false);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        isDMLoaded = false;
        clearGlobeMessage();
        clearDisplayMessage();
        clearDMMessage();
        clearTeamMessage();
        clearUnread();
        NPC_DM_MAP.clear();
        NPC_DM_UNCHECKED.clear();
        ClientDialogProcessHandler.getInstance().clearQuest();
        ClientPictureManager.getInstance().clearRequested();
        Minecraft.getInstance().gui.getChat().getRecentChat().clear();
    }

    private static void clearUnread() {
        CHAT_DM_UNCHECKED.clear();
        NPC_DM_UNCHECKED.clear();
        teamChatChecked = true;
        shouldRotation = false;
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof ChatScreen chatScreen) || event.getScreen() instanceof InBedChatScreen) {
            return;
        }
        String initial = chatScreen.initial;
        //拦截普通聊天界面
        if (initial.isEmpty()) {
            event.setCanceled(true);
            Minecraft.getInstance().setScreen(new MineChatGlobeScreen());
        }
    }

    @SubscribeEvent
    public static void onClientPlayerTick(PlayerTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null || player != event.getEntity()) {
            return;
        }

        if(!isDMLoaded) {
            NPC_DM_MAP.clear();
            ClientChatDataStorage.loadDM();
            isDMLoaded = true;
        }

        autoSaveTime++;
        if(autoSaveTime >= 6000){
            autoSaveTime = 0;
            ClientChatDataStorage.saveDM();
        }

        if(!hasUncheckedMessage() && shouldRotation){
            shouldRotation = false;
        }

        for(AnimationMessage message : CHAT_GLOBE_DISPLAY) {
            message.tick();
            if(message.getAnimationStatus() == AnimationStatus.FINISHED) {
                CHAT_GLOBE_DISPLAY.remove(message);
            }
        }

        if(CHAT_GLOBE_DISPLAY.isEmpty() && !hasUncheckedMessage()) {
            if(showIconTime < ICON_TIME) {
                showIconTime++;
            }
        }
        else if(showIconTime != 0){
            showIconTime = 0;
        }
    }

    public static float getIconDisplayRatio(float partialTick){
        if(showIconTime >= ICON_FADE_TIME){
            return Mth.clamp((showIconTime + partialTick - ICON_FADE_TIME) / (ICON_TIME - ICON_FADE_TIME), 0 , 1F);
        }
        return 0F;
    }

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        LocalPlayer player =  Minecraft.getInstance().player;
        if(level == null || player == null) return;

        Component message = event.getMessage();
        UUID sender = event.getSender();
        MessageType messageType = MessageType.SYSTEM;
        ChatType.Bound boundChatType = event.getBoundChatType();

        if (boundChatType != null) {
            String chatType = boundChatType.chatType().value().chat().translationKey();
            switch (chatType) {
                case "chat.type.announcement" ->{
                    messageType = MessageType.SAY;
                    //System.out.println("消息指令");
                }
                case "chat.type.text" -> {
                    messageType = MessageType.PLAYER_GLOBE;
                    //System.out.println("玩家消息发送");
                }
                case "commands.message.display.outgoing" -> {
                    messageType = MessageType.PLAYER_DM_OUT;
                    //System.out.println("私聊消息发送");
                }
                case "commands.message.display.incoming" -> {
                    messageType = MessageType.PLAYER_DM_IN;
                    //System.out.println("私聊消息接收");
                }
                case "chat.type.team.text" -> {
                    messageType = MessageType.PLAYER_TEAM_IN;
                    //System.out.println("队伍消息接受");
                }
                case "chat.type.team.sent" -> {
                    messageType = MessageType.PLAYER_TEAM_OUT;
                    //System.out.println("队伍消息发送");
                }
                default -> {
                    messageType = MessageType.OTHER;
                    //System.out.println("其他信息");
                }
            }
        }

        if(event instanceof ClientChatReceivedEvent.System systemEvent) {
            if(systemEvent.isOverlay()){
                messageType = MessageType.NOT_SHOWN;
                //System.out.println("系统消息，不显示");
            }
        }

        //加入消息队列
        addMessage(event, player, sender, messageType, message);
    }

    public static void addAnimationMessageToList(ArrayDeque<AnimationMessage> messages, AnimationMessage message) {
        addAnimationMessageToList(messages, message, MAX_SIZE);
    }

    public static void addAnimationMessageToList(ArrayDeque<AnimationMessage> messages, AnimationMessage message, int maxSize) {
        if (messages.size() >= maxSize) {
            messages.pollFirst();
        }
        messages.addLast(message);
    }

    public static void addMessage(ClientChatReceivedEvent event, @NonNull LocalPlayer player, UUID sender, MessageType messageType, Component msg) {
        Screen screen =  Minecraft.getInstance().screen;
        TeamNameInfo nameInfo = getNameLength(sender, messageType);

        long timestamp = player.level().getGameTime();

        switch(messageType) {
            case SYSTEM, OTHER, SAY, PLAYER_GLOBE->{
                boolean isSystem = false;

                if(messageType == MessageType.SYSTEM){
                    isSystem = true;
                    msg = Component.empty()
                            .append(Component.literal("<").withStyle(ChatFormatting.YELLOW))
                            .append(Component.translatable("text.mine_chat.system").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("> ").withStyle(ChatFormatting.YELLOW))
                            .append(msg);
                }
                else if(messageType == MessageType.OTHER){
                    isSystem = true;
                    msg = Component.empty()
                            .append(Component.literal("<"))
                            .append(Component.translatable("text.mine_chat.other"))
                            .append(Component.literal("> "))
                            .append(msg);
                }

                addGlobeMessage(sender, messageType, msg, isSystem, nameInfo.totalLength);
            }
            case PLAYER_DM_IN -> {
                event.setCanceled(true);
                if(event.isSystem()){
                    return;
                }
                //接受消息，发送者是其他玩家，直接处理加入队列
                //格式 XX XX XX 悄悄和你说：XXXX

                //处理掉无用的部分，保留发送者的名字和消息
                MutableComponent message = Component.empty();
                List<Component> components = msg.toFlatList();

                //重新拼接消息
                for(int i = 0; i < components.size(); i++) {
                    if(i == nameInfo.totalLength){
                        continue;
                    }

                    if(i == 0){
                        message.append("<");
                    }

                    message.append(components.get(i).getString());

                    if(i == nameInfo.totalLength - 1){
                        message.append(">");
                    }
                }

                addDMMessage(sender, sender, timestamp, nameInfo.totalLength, message, messageType);
            }
            case PLAYER_DM_OUT -> {
                event.setCanceled(true);
                if(event.isSystem()){
                    return;
                }
                //向其他玩家发送信息，发送者是自身，需要从名字中拿到发送的目标
                //格式 你悄悄和 XX XX XX 说：XXXX

                //处理掉无用的部分，保留发送者的名字和消息
                MutableComponent message = Component.empty();
                String targetName = "";
                List<Component> components = msg.toFlatList();

                //重新拼接消息
                for(int i = 0; i < components.size(); i++) {
                    if(i == 0 || i == nameInfo.totalLength + 1){
                        continue;
                    }

                    //添加括号
                    if(i == 1){
                        message.append("<");
                    }

                    //拼接消息

                    if(i == nameInfo.prefix + nameInfo.nameLength){
                        //取出发送目标的名字
                        targetName = components.get(i).getString();
                        //替换自身的名字
                        message.append(player.getDisplayName());
                    }
                    else{
                        message.append(components.get(i).getString());
                    }

                    //添加括号
                    if(i == nameInfo.totalLength){
                        message.append(">");
                    }
                }

                //名字
                PlayerCache playerCache = PlayerCacheManager.getPlayerCache(targetName, true);
                if (playerCache != null) {
                    //目标玩家的UUID
                    UUID sendTarget = playerCache.getUuid();

                    //发送的目标不能是自己
                    if(!sendTarget.equals(player.getUUID())) {
                        //找到已有或者创建新的
                        Pair<ArrayDeque<AnimationMessage>, Long> dmPair = CHAT_DM_MAP.computeIfAbsent(sendTarget, k -> new Pair<>(new ArrayDeque<>(), timestamp));
                        ArrayDeque<AnimationMessage> dmMessages = dmPair.getFirst();

                        boolean isInDMScreen = screen instanceof MineChatDMScreen;

                        ChatSender chatSender = new ChatSender(sender, playerCache.getName(), null, SenderType.PLAYER);

                        addAnimationMessageToList(dmMessages, new AnimationMessage(chatSender, timestamp, nameInfo.totalLength + 2, messageType, isInDMScreen ? 5 : 0 , 0, 0, message));
                        CHAT_DM_MAP.put(sendTarget, new Pair<>(dmMessages, timestamp));

                        ClientChatDataStorage.savePlayerDM();
                    }
                }
            }
            case PLAYER_TEAM_IN, PLAYER_TEAM_OUT -> {
                if(event.isSystem()){
                    return;
                }
                List<Component> components = msg.toFlatList();
                int skip = messageType == MessageType.PLAYER_TEAM_OUT ? 4 : 3;
                MutableComponent message = Component.empty();
                components.stream().skip(skip).forEach(message::append);

                boolean isInTeamScreen = screen instanceof MineChatTeamScreen;

                ChatSender chatSender = new ChatSender(sender, "", null, SenderType.PLAYER);
                addAnimationMessageToList(CHAT_TEAM, new AnimationMessage(chatSender, timestamp, nameInfo.totalLength, messageType, isInTeamScreen ? 5 : 0 , 0, 0, message));

                if(!isInTeamScreen) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MineChatSounds.RECEIVE_MESSAGE, 1.0F));
                    teamChatChecked = false;
                    setRandomChanceRotation();
                }
            }
        }
    }

    public static void addGlobeMessage(UUID sender, MessageType messageType, Component msg, boolean isSystem, int totalLength) {
        boolean isInGlobeScreen = Minecraft.getInstance().screen instanceof MineChatGlobeScreen;

        ClientLevel level = Minecraft.getInstance().level;

        long timestamp = 0;
        if(level != null){
            timestamp = level.getGameTime();
        }

        ChatSender chatSender = new ChatSender(sender, null, null, isSystem ? SenderType.SYSTEM : SenderType.PLAYER);

        AnimationMessage message = new AnimationMessage(chatSender, timestamp, totalLength, messageType, msg);

        //加入全局历史消息
        addAnimationMessageToList(CHAT_GLOBE ,new AnimationMessage(chatSender, timestamp, totalLength, messageType, isInGlobeScreen ? 5 : 0 , 0, 0, msg));
        addAnimationMessageToList(CHAT_GLOBE_DISPLAY , message, MineChatClientConfig.MAX_DISPLAYED_MESSAGES.getAsInt());

        if(!isInGlobeScreen){
            if(message.isHasMention() && !message.isMentionRead()){
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MineChatSounds.RECEIVE_MESSAGE, 1.0F));
                uncheckedPingMessage = true;
            }
        }
    }

    public static void addDMMessage(UUID sender, UUID target, long timestamp,int nameLength, Component message, MessageType messageType) {
        //找到已有或者创建新的
        Pair<ArrayDeque<AnimationMessage>, Long> dmPair = CHAT_DM_MAP.computeIfAbsent(sender, k -> new Pair<>(new ArrayDeque<>(), timestamp));
        ArrayDeque<AnimationMessage> dmMessages = dmPair.getFirst();

        Screen screen = Minecraft.getInstance().screen;
        boolean isInDMScreen = screen instanceof MineChatDMScreen;

        PlayerCache playerCache = PlayerCacheManager.getPlayerCache(sender);
        ChatSender chatSender = new ChatSender(sender, playerCache != null ? playerCache.getName() : "unknow", null, SenderType.PLAYER);
        addAnimationMessageToList(dmMessages, new AnimationMessage(chatSender, timestamp, nameLength + 2, messageType, isInDMScreen ? 5 : 0 , 0, 0, message));
        CHAT_DM_MAP.put(target, new Pair<>(dmMessages, timestamp));

        setDMMessageCheckStatus(target, false);
        ClientChatDataStorage.savePlayerDM();
    }

    public static void setDMMessageCheckStatus(UUID sender, boolean npc) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof MineChatDMScreen dmScreen) || !sender.equals(dmScreen.getSelectedTarget())) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MineChatSounds.RECEIVE_MESSAGE, 1.0F));
            if(npc){
                NPC_DM_UNCHECKED.add(sender);
            }
            else {
                CHAT_DM_UNCHECKED.add(sender);
            }
            setRandomChanceRotation();
        }
        if (screen instanceof MineChatDMScreen dmScreen && dmScreen.getSearchText().isEmpty()) {
            dmScreen.reflashInfo();
        }
    }


    public static void sendToNPC(LocalPlayer player, UUID npc, Component message) {
        TeamNameInfo nameInfo = getNameLength(player.getUUID(), MessageType.NPC_DIALOG);
        message = Component.empty().append("<").append(player.getDisplayName()).append(">").append(message);
        long gameTime = player.level().getGameTime();
        ArrayDeque<AnimationMessage> animationMessages = new ArrayDeque<>();
        animationMessages.add(new AnimationMessage(
                new ChatSender(player.getUUID(), player.getName().getString(), null, SenderType.PLAYER),
                gameTime, nameInfo.getTotalLength(), MessageType.PLAYER_DM_OUT, 5, 0,0, message
        ));
        NPC_DM_MAP.computeIfAbsent(npc, k -> new Pair<>(new ArrayDeque<>(), gameTime)).getFirst().addAll(animationMessages);
        ClientChatDataStorage.saveNPCDM();
    }

    public static void sendNPCMessage(@NonNull ChatSender sender, Component message, long timestamp) {
        message = Component.empty().append("<").append(Component.translatable(sender.getName() == null ? "Unknow" : sender.getName())).append(">").append(message);

        //找到已有或者创建新的
        Pair<ArrayDeque<AnimationMessage>, Long> dmPair = NPC_DM_MAP.computeIfAbsent(sender.getUuid(), k -> new Pair<>(new ArrayDeque<>(), timestamp));
        ArrayDeque<AnimationMessage> dmMessages = dmPair.getFirst();

        Screen screen = Minecraft.getInstance().screen;
        boolean isInDMScreen = screen instanceof MineChatDMScreen;

        addAnimationMessageToList(dmMessages, new AnimationMessage(sender, timestamp, 3, MessageType.PLAYER_DM_IN, isInDMScreen ? 5 : 0 , 0, 0, message));
        NPC_DM_MAP.put(sender.getUuid(), new Pair<>(dmMessages, timestamp));
        setDMMessageCheckStatus(sender.getUuid(), true);
        ClientChatDataStorage.saveNPCDM();
    }

    public static void setRandomChanceRotation(){
        double chance = Math.random();
        if(chance < 0.1){
            shouldRotation = true;
        }
    }



    public static boolean isServerInstalled() {
        return isServerInstalled;
    }

    public static void setServerInstalled(boolean serverInstalled) {
        isServerInstalled = serverInstalled;
    }

    public static boolean shouldRotation(){
        return shouldRotation;
    }

    public static TeamNameInfo getNameLength(UUID sender, MessageType messageType) {
        int nameLength = 3;
        int prefix = 0;
        int suffix = 0;

        if(messageType == MessageType.PLAYER_DM_IN || messageType == MessageType.PLAYER_DM_OUT) {
            nameLength = 1;
        }

        PlayerInfo playerInfo = PlayerCacheManager.getPlayerInfo(sender);
        if (playerInfo != null) {
            PlayerTeam team = playerInfo.getTeam();
            if (team != null) {
                prefix = team.getPlayerPrefix().getString().isEmpty() ? 0 : 1;
                suffix = team.getPlayerSuffix().getString().isEmpty() ? 0 : 1;
            }
        }
        return new TeamNameInfo(nameLength, prefix, suffix);
    }

    public static List<ChatTarget> getChatTargets(String name) {
        List<ChatTarget> targets = new ArrayList<>();

        boolean searching = name != null && !name.isEmpty();
        String searchName = searching ? name.toLowerCase() : "";

        Set<UUID> addedPlayerUUIDs = new HashSet<>();


        CHAT_DM_MAP.forEach((uuid, value) -> {
            PlayerCache cache = PlayerCacheManager.getPlayerCache(uuid);
            if (cache == null) {
                return;
            }

            if (searching) {
                String playerName = cache.getName();
                if (playerName == null || !playerName.toLowerCase().startsWith(searchName)) {
                    return;
                }
            }

            long lastMessageTime = value.getSecond();
            targets.add(new ChatTarget(null, cache, lastMessageTime));
            addedPlayerUUIDs.add(uuid);
        });


        if (searching) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                mc.getConnection().getOnlinePlayers().forEach(playerInfo -> {
                    GameProfile profile = playerInfo.getProfile();
                    UUID uuid = profile.getId();
                    String playerName = profile.getName();

                    if (playerName == null || !playerName.toLowerCase().startsWith(searchName)) {
                        return;
                    }

                    if (addedPlayerUUIDs.contains(uuid)) {
                        return;
                    }

                    PlayerCache cache = PlayerCacheManager.getPlayerCache(uuid);

                    if (cache == null) {
                        return;
                    }

                    targets.add(new ChatTarget(null, cache, 0L));
                    addedPlayerUUIDs.add(uuid);
                });
            }
        }


        NPC_DM_MAP.forEach((uuid, value) -> {
            ChatSender sender = NPCSenderManager.getInstance().getNpcData(uuid);
            if (sender == null) {
                return;
            }

            if (searching) {
                String npcName = sender.getName();
                if (npcName == null || !npcName.toLowerCase().startsWith(searchName)) {
                    return;
                }
            }

            long lastMessageTime = value.getSecond();
            targets.add(new ChatTarget(sender, null, lastMessageTime));
        });


        targets.sort(Comparator.comparingLong(ChatTarget::getLastMessageTime).reversed());

        return targets;
    }

    public static boolean hasUncheckedMessage(){
        return isPingUnchecked() || isDMChatUnchecked() || isTeamChatUnchecked() || isNPChatUnchecked();
    }

    public static boolean isNPChatUnchecked() {
        return !NPC_DM_UNCHECKED.isEmpty();
    }

    public static boolean isTeamChatUnchecked(){
        return !teamChatChecked;
    }

    public static boolean isDMChatUnchecked(){
        return !CHAT_DM_UNCHECKED.isEmpty();
    }

    public static boolean isPingUnchecked(){
        return uncheckedPingMessage;
    }

    public static void checkPingMessage(){
        uncheckedPingMessage = false;
    }

    public static void checkDM(UUID uuid, boolean isPlayer){
        if(isPlayer){
            if(CHAT_DM_MAP.containsKey(uuid)){
                CHAT_DM_UNCHECKED.remove(uuid);
            }
        }
        else {
            if(NPC_DM_MAP.containsKey(uuid)){
                NPC_DM_UNCHECKED.remove(uuid);
            }
        }
    }

    public static boolean isDMPlayerMessageUnread(UUID id) {
        return CHAT_DM_UNCHECKED.contains(id);
    }

    public static boolean isNPCMessageUnread(UUID id) {
        return NPC_DM_UNCHECKED.contains(id);
    }

    public static void checkTeam(){
        teamChatChecked = true;
    }

    public static List<AnimationMessage> getGlobeMessages(){
        return new ArrayList<>(CHAT_GLOBE);
    }

    public static List<AnimationMessage> getTeamMessages() {
        return new ArrayList<>(CHAT_TEAM);
    }

    public static List<AnimationMessage> getDMMessages(UUID target) {
        return new ArrayList<>(CHAT_DM_MAP.getOrDefault(target, new Pair<>(new ArrayDeque<>(), 0L)).getFirst());
    }

    public static List<AnimationMessage> getLatestGlobeMessages(){
        return new ArrayList<>(CHAT_GLOBE_DISPLAY);
    }

    public static void clearGlobeMessage() {
        CHAT_GLOBE.clear();
    }

    public static void clearDisplayMessage() {
        CHAT_GLOBE_DISPLAY.clear();
    }

    public static void clearDMMessage() {
        CHAT_DM_MAP.clear();
    }

    public static void clearTeamMessage() {
        CHAT_TEAM.clear();
    }

    public static ArrayDeque<AnimationMessage> getChatGlobe(){
        return CHAT_GLOBE;
    }

    public static ArrayDeque<AnimationMessage> getChatGlobeDisplay(){
        return CHAT_GLOBE_DISPLAY;
    }

    public static Map<UUID, Pair<ArrayDeque<AnimationMessage>, Long>> getDMMap() {
        return CHAT_DM_MAP;
    }

    public static List<AnimationMessage> getNPCMessages(UUID targetUUID) {
        List<AnimationMessage> npcMessages = new ArrayList<>();
        if (NPC_DM_MAP.containsKey(targetUUID)) {
            npcMessages.addAll(NPC_DM_MAP.getOrDefault(targetUUID, new Pair<>(new ArrayDeque<>(), 0L)).getFirst());
        }
        return npcMessages;
    }

    public static Map<UUID, Pair<ArrayDeque<AnimationMessage>, Long>> getNPCMap() {
        return NPC_DM_MAP;
    }

    public static void modifyLatestNPCMessage(@NotNull UUID uuid, Component component, boolean resetAnimation) {
        Pair<ArrayDeque<AnimationMessage>, Long> pair = NPC_DM_MAP.get(uuid);
        if(pair != null){
            ArrayDeque<AnimationMessage> messages = pair.getFirst();
            Iterator<AnimationMessage> iterator = messages.descendingIterator();
            while (iterator.hasNext()) {
                AnimationMessage msg = iterator.next();
                if (msg.getSender().getUuid().equals(uuid)) {
                    MutableComponent senderName = msg.getDisplayMessage().senderName();
                    msg.setMessage(senderName.append(component));
                    if(resetAnimation){
                        msg.setChangeAnimation(true);
                    }
                    return;
                }
            }
        }
    }


    public static class TeamNameInfo{
        int nameLength;
        int prefix;
        int suffix;
        int totalLength;

        public TeamNameInfo(int nameLength, int prefix, int suffix) {
            this.nameLength = nameLength;
            this.prefix = prefix;
            this.suffix = suffix;
            this.totalLength = nameLength + prefix + suffix;
        }

        public int getNameLength() {
            return nameLength;
        }

        public int getPrefix() {
            return prefix;
        }

        public int getSuffix() {
            return suffix;
        }

        public int getTotalLength() {
            return totalLength;
        }
    }

    public static class ChatTarget {

        private final ChatSender sender;
        private final PlayerCache playerCache;
        private final long lastMessageTime;

        public ChatTarget(
                ChatSender sender,
                PlayerCache playerCache,
                long lastMessageTime
        ) {
            this.sender = sender;
            this.playerCache = playerCache;
            this.lastMessageTime = lastMessageTime;
        }

        public ChatSender getSender() {
            return sender;
        }

        public PlayerCache getPlayerCache() {
            return playerCache;
        }

        public long getLastMessageTime() {
            return lastMessageTime;
        }

        public boolean isPlayer() {
            return playerCache != null;
        }
    }
}
