package com.hyisnoob.realmfinder.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PhotoStandBlockEntity extends BlockEntity {

    private ItemStack photo = ItemStack.EMPTY;

    public PhotoStandBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlocks.PHOTO_STAND_BE, pos, blockState);
    }

    public boolean hasPhoto() {
        return !photo.isEmpty();
    }

    public ItemStack getPhoto() {
        return photo;
    }

    public void setPhoto(ItemStack stack) {
        this.photo = stack.copy();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Photo", 10)) {
            this.photo = ItemStack.parseOptional(registries, tag.getCompound("Photo"));
        } else {
            this.photo = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!photo.isEmpty()) {
            tag.put("Photo", photo.save(registries, new CompoundTag()));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!photo.isEmpty()) {
            tag.put("Photo", photo.save(registries, new CompoundTag()));
        }
        return tag;
    }
}
