package com.hyisnoob.realmfinder.core.engine;

import com.hyisnoob.realmfinder.core.config.RealmFinderConfig;
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

    public static void clear() {
        PLAYER_UNDO_STACKS.clear();
    }

    public static void pushUndo(UUID playerId, UndoRecord record) {
        Deque<UndoRecord> stack = PLAYER_UNDO_STACKS.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        stack.push(record);
        while (stack.size() > RealmFinderConfig.get().maxUndoSteps) stack.removeLast();
    }

    public static void enforceLimit() {
        for (Deque<UndoRecord> stack : PLAYER_UNDO_STACKS.values()) {
            while (stack.size() > RealmFinderConfig.get().maxUndoSteps) stack.removeLast();
        }
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

        UndoRecord record = stack.peek();
        boolean success = record.restore(player.serverLevel(), player);

        if (success) {
            stack.pop();
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
        } else {
            if (record.failureReason() == UndoRecord.FailureReason.CONFLICT && record.conflictPos() != null) {
                var pos = record.conflictPos();
                player.displayClientMessage(Component.translatable("message.realmfinder.undo_conflict",
                        pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.RED), true);
                return false;
            }
            String key = switch (record.failureReason()) {
                case DIMENSION -> "message.realmfinder.undo_dimension";
                case CHUNK -> "message.realmfinder.undo_chunk";
                default -> "message.realmfinder.undo_rejected";
            };
            player.displayClientMessage(Component.translatable(key)
                    .withStyle(ChatFormatting.RED), true);
        }

        return success;
    }
}
