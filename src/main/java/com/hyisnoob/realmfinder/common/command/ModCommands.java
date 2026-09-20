package com.hyisnoob.realmfinder.common.command;

import com.hyisnoob.realmfinder.core.engine.UndoManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class ModCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });
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
