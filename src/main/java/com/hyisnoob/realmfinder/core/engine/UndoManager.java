package com.hyisnoob.realmfinder.core.engine;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UndoManager {

    private static final Map<UUID, Deque<UndoRecord>> PLAYER_UNDO_STACKS = new ConcurrentHashMap<>();
    private static final int MAX_UNDO_PER_PLAYER = 10;

    public static void pushUndo(UUID playerId, UndoRecord record) {
        Deque<UndoRecord> stack = PLAYER_UNDO_STACKS.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        if (stack.size() >= MAX_UNDO_PER_PLAYER) {
            stack.removeLast();
        }
        stack.push(record);
    }

    public static boolean undo(ServerPlayer player) {
        Deque<UndoRecord> stack = PLAYER_UNDO_STACKS.get(player.getUUID());
        if (stack == null || stack.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.realmfinder.undo_empty").withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        UndoRecord record = stack.pop();
        boolean success = record.restore(player.serverLevel(), player);

        if (success) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);

            player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getEyeY(), player.getZ(),
                    60, 1.0, 1.0, 1.0, 0.2);

            player.displayClientMessage(
                    Component.translatable("message.realmfinder.undo_success", record.getModifiedBlockCount())
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
        }

        return success;
    }
}
