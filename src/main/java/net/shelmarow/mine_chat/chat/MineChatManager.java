package net.shelmarow.mine_chat.chat;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.mine_chat.MineChat;
import net.shelmarow.mine_chat.chat.message.AnimationMessage;
import net.shelmarow.mine_chat.chat.message.chat_enum.AnimationStatus;
import net.shelmarow.mine_chat.chat.message.chat_enum.MessageType;
import net.shelmarow.mine_chat.chat.playercache.PlayerCache;
import net.shelmarow.mine_chat.chat.playercache.PlayerCacheManager;
import net.shelmarow.mine_chat.chat.screen.MineChatDMScreen;
import net.shelmarow.mine_chat.chat.screen.MineChatGlobeScreen;
import net.shelmarow.mine_chat.chat.screen.MineChatTeamScreen;
import net.shelmarow.mine_chat.chat.sound.MineChatSounds;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;

@Mod.EventBusSubscriber(modid = MineChat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MineChatManager {
    //最大可记录的消息条数
    private static final int MAX_SIZE = 200;
    private static final int MAX_DISPLAY_SIZE = 3;
    //全局消息
    private static final ArrayDeque<AnimationMessage> CHAT_GLOBE = new ArrayDeque<>();
    private static final ArrayDeque<AnimationMessage> CHAT_GLOBE_DISPLAY = new ArrayDeque<>();
    //私聊消息
    private static final Map<UUID, Pair<ArrayDeque<AnimationMessage>, Long>> CHAT_DM_MAP = new HashMap<>();
    //未确认的私聊消息名单
    private static final Set<UUID> CHAT_DM_UNCHECKED = new HashSet<>();
    //队伍消息
    private static final ArrayDeque<AnimationMessage> CHAT_TEAM = new ArrayDeque<>();
    //是否确认过队伍消息
    private static boolean teamChatChecked = true;

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clearGlobeMessage();
        clearDisplayMessage();
        clearDMMessage();
        clearTeamMessage();
        clearUnread();
        PlayerCacheManager.clearCache();
    }

    private static void clearUnread() {
        CHAT_DM_UNCHECKED.clear();
        teamChatChecked = true;
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof ChatScreen chatScreen)) {
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
    public static void onChatReceived(ClientChatReceivedEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        LocalPlayer player =  Minecraft.getInstance().player;
        if(level == null || player == null) return;

        Component message = event.getMessage();
        UUID sender = event.getSender();
        MessageType messageType;

        String chatType = event.getBoundChatType().chatType().chat().translationKey();
        switch (chatType) {
            case "chat.type.announcement" ->{
                messageType = MessageType.SAY;
                System.out.println("消息指令");
            }
            case "chat.type.text" -> {
                messageType = MessageType.PLAYER_GLOBE;
                System.out.println("玩家消息发送");
            }
            case "commands.message.display.outgoing" -> {
                messageType = MessageType.PLAYER_DM_OUT;
                System.out.println("私聊消息发送");
            }
            case "commands.message.display.incoming" -> {
                messageType = MessageType.PLAYER_DM_IN;
                System.out.println("私聊消息接收");
            }
            case "chat.type.team.text" -> {
                messageType = MessageType.PLAYER_TEAM_IN;
                System.out.println("队伍消息接受");
            }
            case "chat.type.team.sent" -> {
                messageType = MessageType.PLAYER_TEAM_OUT;
                System.out.println("队伍消息发送");
            }
            default -> {
                if(event instanceof ClientChatReceivedEvent.System systemEvent) {
                    if(systemEvent.isOverlay()){
                        messageType = MessageType.NOT_SHOWN;
                        System.out.println("系统消息，不显示");
                    }
                    else{
                        messageType = MessageType.SYSTEM;
                        System.out.println("系统消息");
                    }
                }
                else{
                    messageType = MessageType.OTHER;
                    System.out.println("其他信息");
                }
            }
        }

        //加入消息队列
        addMessage(event, player, sender, messageType, message);
    }

    @SubscribeEvent
    public static void onClientPlayerTick(TickEvent.PlayerTickEvent event) {
        if(event.side == LogicalSide.SERVER || Minecraft.getInstance().player == null || Minecraft.getInstance().player != event.player) {
            return;
        }
        for(AnimationMessage message : CHAT_GLOBE_DISPLAY) {
            message.tick();
            if(message.getAnimationStatus() == AnimationStatus.FINISHED) {
                CHAT_GLOBE_DISPLAY.remove(message);
            }
        }
    }

    private static void addAnimationMessageToList(ArrayDeque<AnimationMessage> messages, AnimationMessage message) {
        addAnimationMessageToList(messages, message, MAX_SIZE);
    }

    private static void addAnimationMessageToList(ArrayDeque<AnimationMessage> messages, AnimationMessage message, int maxSize) {
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
                if(messageType == MessageType.SYSTEM){
                    msg = Component.empty()
                            .append(Component.literal("<").withStyle(ChatFormatting.YELLOW))
                            .append(Component.translatable("text.mine_chat.system").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("> ").withStyle(ChatFormatting.YELLOW))
                            .append(msg);
                }
                else if(messageType == MessageType.OTHER){
                    msg = Component.empty()
                            .append(Component.literal("<"))
                            .append(Component.translatable("text.mine_chat.other"))
                            .append(Component.literal("> "))
                            .append(msg);
                }

                boolean isInGlobeScreen = screen instanceof MineChatGlobeScreen;

                //加入全局历史消息
                addAnimationMessageToList(CHAT_GLOBE ,new AnimationMessage(sender, timestamp, nameInfo.totalLength, messageType, isInGlobeScreen ? 5 : 0 , 0, 0, msg));

                //加入全局最新消息显示队列
                addAnimationMessageToList(CHAT_GLOBE_DISPLAY , new AnimationMessage(sender, timestamp, nameInfo.totalLength, messageType, msg), MAX_DISPLAY_SIZE);
            }
            case PLAYER_DM_IN -> {
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

                //找到已有或者创建新的
                Pair<ArrayDeque<AnimationMessage>, Long> dmPair = CHAT_DM_MAP.computeIfAbsent(sender, k -> new Pair<>(new ArrayDeque<>(), timestamp));
                ArrayDeque<AnimationMessage> dmMessages = dmPair.getFirst();

                boolean isInDMScreen = screen instanceof MineChatDMScreen;
                addAnimationMessageToList(dmMessages, new AnimationMessage(sender, timestamp, nameInfo.totalLength + 2, messageType, isInDMScreen ? 5 : 0 , 0, 0, message));
                CHAT_DM_MAP.put(sender, new Pair<>(dmMessages, timestamp));

                if(!(screen instanceof MineChatDMScreen dmScreen) || !sender.equals(dmScreen.getSelectedTarget())){
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MineChatSounds.RECEIVE_MESSAGE.get(), 1.0F));
                    CHAT_DM_UNCHECKED.add(sender);
                }
                if(screen instanceof MineChatDMScreen dmScreen && dmScreen.getSearchText().isEmpty()){
                    dmScreen.reflashPlayerInfo();
                }
                event.setCanceled(true);
            }
            case PLAYER_DM_OUT -> {
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
                    UUID sendTarget = playerCache.getProfile().getId();

                    //发送的目标不能是自己
                    if(!sendTarget.equals(player.getUUID())) {
                        //找到已有或者创建新的
                        Pair<ArrayDeque<AnimationMessage>, Long> dmPair = CHAT_DM_MAP.computeIfAbsent(sendTarget, k -> new Pair<>(new ArrayDeque<>(), timestamp));
                        ArrayDeque<AnimationMessage> dmMessages = dmPair.getFirst();

                        boolean isInDMScreen = screen instanceof MineChatDMScreen;
                        addAnimationMessageToList(dmMessages, new AnimationMessage(sender, timestamp, nameInfo.totalLength + 2, messageType, isInDMScreen ? 5 : 0 , 0, 0, message));
                        CHAT_DM_MAP.put(sendTarget, new Pair<>(dmMessages, timestamp));
                    }
                }
                event.setCanceled(true);
            }
            case PLAYER_TEAM_IN, PLAYER_TEAM_OUT -> {
                List<Component> components = msg.toFlatList();
                int skip = messageType == MessageType.PLAYER_TEAM_OUT ? 4 : 3;
                MutableComponent message = Component.empty();
                components.stream().skip(skip).forEach(message::append);

                boolean isInTeamScreen = screen instanceof MineChatTeamScreen;
                addAnimationMessageToList(CHAT_TEAM, new AnimationMessage(sender, timestamp, nameInfo.totalLength, messageType, isInTeamScreen ? 5 : 0 , 0, 0, message));

                if(!isInTeamScreen) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(MineChatSounds.RECEIVE_MESSAGE.get(), 1.0F));
                    teamChatChecked = false;
                }
            }
        }
    }

    private static TeamNameInfo getNameLength(UUID sender, MessageType messageType) {
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

    public static List<PlayerCache> getDMPlayers(String name){
        List<PlayerCache> playerCaches = new ArrayList<>();

        if (name == null || name.isEmpty()) {
            CHAT_DM_MAP.entrySet().stream()
                    // 按时间戳倒序（越大越靠前）
                    .sorted((e1, e2) -> Long.compare(
                            e2.getValue().getSecond(),
                            e1.getValue().getSecond()
                    ))
                    .forEach(entry -> {
                        UUID uuid = entry.getKey();
                        PlayerCache cache = PlayerCacheManager.getPlayerCache(uuid);
                        if (cache != null) {
                            playerCaches.add(cache);
                        }
                    });
        } else {
            playerCaches.addAll(PlayerCacheManager.getPlayerCachesByName(name));
        }

        return playerCaches;
    }

    public static boolean hasUncheckedMessage(){
        return isDMChatUnchecked() || isTeamChatUnchecked();
    }

    public static boolean isTeamChatUnchecked(){
        return !teamChatChecked;
    }

    public static boolean isDMChatUnchecked(){
        return !CHAT_DM_UNCHECKED.isEmpty();
    }

    public static void checkDM(UUID uuid){
        if(CHAT_DM_MAP.containsKey(uuid)){
            CHAT_DM_UNCHECKED.remove(uuid);
        }
    }

    public static boolean isDMPlayerMessageUnread(UUID id) {
        return CHAT_DM_UNCHECKED.contains(id);
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
    }
}
