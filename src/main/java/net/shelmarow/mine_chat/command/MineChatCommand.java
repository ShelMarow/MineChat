package net.shelmarow.mine_chat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.shelmarow.mine_chat.chat.npc.NPCDialog;
import net.shelmarow.mine_chat.chat.npc.NPCDialogManager;
import net.shelmarow.mine_chat.chat.npc.NPCDialogRegister;

import java.util.List;
import java.util.UUID;

public class MineChatCommand {

    // 创建建议提供者
    private static final SuggestionProvider<CommandSourceStack> DIALOG_SUGGESTIONS =
            (context, builder) -> {
                List<String> idList = NPCDialogRegister.listID();
                return SharedSuggestionProvider.suggest(idList, builder);
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mine_chat")
                .requires(sourceStack-> sourceStack.hasPermission(2))
                .then(Commands.literal("dialog")
                        .then(Commands.literal("send")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .suggests(DIALOG_SUGGESTIONS)
                                        .executes(context -> {
                                            String id = StringArgumentType.getString(context, "id");
                                            NPCDialog npcDialog = NPCDialogRegister.getNPCDialog(id);
                                            if(npcDialog != null){
                                                UUID uuid = npcDialog.getChatSender().getUuid();
                                                NPCDialogManager.getInstance().addNPCDialogQuest(uuid, context.getSource().getPlayer(), npcDialog, true);
                                                context.getSource().sendSuccess(() -> Component.literal("已发送NPC消息给玩家"), false);
                                            }
                                            else {
                                                context.getSource().sendFailure(Component.literal("NPC消息不存在！"));
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("list")
                                .executes(ctx->{
                                    List<String> idList = NPCDialogRegister.listID();

                                    for (String id : idList) {
                                        ctx.getSource().sendSuccess(() -> Component.literal(id), false);
                                    }

                                    return 1;
                                })
                        )
                )
        );
    }
}
