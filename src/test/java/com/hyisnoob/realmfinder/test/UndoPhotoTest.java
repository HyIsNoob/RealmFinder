package com.hyisnoob.realmfinder.test;

import com.hyisnoob.realmfinder.core.engine.UndoRecord;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndoPhotoTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void survivalUndoReturnsOnlyOnePhotoFromAStack() {
        UndoRecord record = new UndoRecord(new ItemStack(Items.PAPER, 8), true);
        assertEquals(1, record.photoToReturn().getCount());
    }

    @Test
    void creativeUndoDoesNotMintAPhoto() {
        UndoRecord record = new UndoRecord(new ItemStack(Items.PAPER, 8), false);
        assertTrue(record.photoToReturn().isEmpty());
    }

    @Test
    void openingDoorDoesNotPreventUndoButBreakingItDoes() {
        var closed = Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.OPEN, false);
        var opened = closed.setValue(BlockStateProperties.OPEN, true);
        assertTrue(UndoRecord.sameBlockForUndo(closed, opened));
        assertTrue(!UndoRecord.sameBlockForUndo(closed, Blocks.AIR.defaultBlockState()));
        assertTrue(!UndoRecord.sameBlockForUndo(closed, Blocks.STONE.defaultBlockState()));
    }

    @Test
    void missingFragileBlockAndAlreadyRestoredTerrainDoNotBlockUndo() {
        assertTrue(UndoRecord.mayRestore(Blocks.OAK_DOOR.defaultBlockState(),
                Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState()));
        assertTrue(UndoRecord.mayRestore(Blocks.DIRT.defaultBlockState(),
                Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState()));
        assertTrue(UndoRecord.mayRestore(Blocks.STONE.defaultBlockState(),
                Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState()));
        assertTrue(!UndoRecord.mayRestore(Blocks.STONE.defaultBlockState(),
                Blocks.AIR.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState()));
    }

    @Test
    void placementPhysicsDoNotMakeImmediateUndoImpossible() {
        assertTrue(UndoRecord.mayRestore(Blocks.DIRT_PATH.defaultBlockState(),
                Blocks.AIR.defaultBlockState(), Blocks.DIRT.defaultBlockState()));
        assertTrue(UndoRecord.mayRestore(Blocks.FARMLAND.defaultBlockState(),
                Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState()));
        assertTrue(UndoRecord.mayRestore(Blocks.SAND.defaultBlockState(),
                Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState()));
        assertTrue(!UndoRecord.mayRestore(Blocks.STONE.defaultBlockState(),
                Blocks.AIR.defaultBlockState(), Blocks.DIRT.defaultBlockState()));
    }

    @Test
    void undoClearsPlacedContainerBeforeReplacingItsBlock() {
        ChestBlockEntity chest = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        chest.setItem(0, new ItemStack(Items.DIAMOND, 8));

        UndoRecord.clearContainerBeforeReplacement(chest);

        assertTrue(chest.isEmpty());
    }
}
