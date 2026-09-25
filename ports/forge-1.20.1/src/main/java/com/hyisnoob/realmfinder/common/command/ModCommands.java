package com.hyisnoob.realmfinder.common.command;

import com.hyisnoob.realmfinder.core.engine.UndoManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

public class ModCommands {

    public static void register(RegisterCommandsEvent event) {
        registerCommands(event.getDispatcher());
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("realmfinder")
                        .then(Commands.literal("undo")
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        boolean success = UndoManager.undo(player);
                                        return success ? 1 : 0;
                                    }
                                    return 0;
                                })
                        )
        );
    }
}
