package com.hyisnoob.realmfinder.common.block;

import com.hyisnoob.realmfinder.common.item.ModItems;
import com.hyisnoob.realmfinder.core.engine.UndoManager;
import com.hyisnoob.realmfinder.core.engine.UndoRecord;
import com.hyisnoob.realmfinder.core.engine.ViewfinderEngine;
import com.hyisnoob.realmfinder.core.math.CameraTransform;
import com.hyisnoob.realmfinder.core.service.PlatformHelper;
import com.hyisnoob.realmfinder.core.snapshot.SnapshotSerializer;
import com.hyisnoob.realmfinder.core.snapshot.WorldSnapshot;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PhotoStandBlock extends BaseEntityBlock {

    public static final MapCodec<PhotoStandBlock> CODEC = simpleCodec(PhotoStandBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public PhotoStandBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhotoStandBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof PhotoStandBlockEntity standEntity) {
            // 1. Sneak + Click with item: trigger photo materialization if photo present
            if (player.isShiftKeyDown() && standEntity.hasPhoto()) {
                materializePhoto(level, state, pos, player, standEntity);
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            // 2. Click with Photograph into empty stand: mount photo
            if (!standEntity.hasPhoto() && stack.is(ModItems.PHOTOGRAPH)) {
                if (!level.isClientSide) {
                    standEntity.setPhoto(stack.split(1));
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof PhotoStandBlockEntity standEntity) {
            // 1. Sneak + Click with empty hand: trigger photo materialization
            if (player.isShiftKeyDown() && standEntity.hasPhoto()) {
                materializePhoto(level, state, pos, player, standEntity);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // 2. Click with empty hand on stand with photo: retrieve photo
            if (standEntity.hasPhoto()) {
                if (!level.isClientSide) {
                    ItemStack retrieved = standEntity.getPhoto().copy();
                    standEntity.setPhoto(ItemStack.EMPTY);
                    if (!player.addItem(retrieved)) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, retrieved);
                    }
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    private void materializePhoto(Level level, BlockState state, BlockPos pos, Player player, PhotoStandBlockEntity standEntity) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ItemStack photo = standEntity.getPhoto();
            CompoundTag tag = PlatformHelper.getCustomTag(photo);
            WorldSnapshot snapshot = SnapshotSerializer.fromNbt(tag);
            if (snapshot != null) {
                Direction facing = state.getValue(FACING);
                double targetX = pos.getX() + 0.5 + facing.getStepX() * 2.5;
                double targetY = pos.getY() + 0.5;
                double targetZ = pos.getZ() + 0.5 + facing.getStepZ() * 2.5;
                float targetYaw = facing.toYRot();

                CameraTransform targetCamera = new CameraTransform(
                        targetX, targetY, targetZ,
                        targetYaw, 0.0f
                );

                UndoRecord undoRecord = new UndoRecord(photo);
                ViewfinderEngine.stamp(serverPlayer.serverLevel(), snapshot, targetCamera, false, 1.0f, undoRecord);
                UndoManager.pushUndo(serverPlayer.getUUID(), undoRecord);

                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8f, 1.8f);
                serverPlayer.displayClientMessage(Component.translatable("message.realmfinder.stamped"), true);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof PhotoStandBlockEntity standEntity && standEntity.hasPhoto()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, standEntity.getPhoto());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
