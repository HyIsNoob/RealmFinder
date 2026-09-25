package com.hyisnoob.realmfinder.test;

import com.hyisnoob.realmfinder.core.engine.PlacementGeometry;
import com.hyisnoob.realmfinder.core.math.CameraTransform;
import com.hyisnoob.realmfinder.core.snapshot.CapturedBlock;
import com.hyisnoob.realmfinder.core.snapshot.WorldSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlacementGeometryTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void farMovesTheUnscaledVoxelFartherAway() {
        WorldSnapshot snapshot = WorldSnapshot.createNew(UUID.randomUUID(), 40, 1, 1, 36, 0, 0);
        snapshot.getOrAddPaletteIndex(Blocks.STONE.defaultBlockState());
        snapshot.getBlocks().add(new CapturedBlock(-0.5f, -0.5f, 5.5f, 0, null));
        CameraTransform target = new CameraTransform(0, 65, 0, 0, 0);

        PlacementGeometry.Plan normal = PlacementGeometry.build(snapshot, target, 1, -64, 320);
        assertTrue(normal.blocks().containsKey(new BlockPos(0, 64, 5)));
        assertEquals(1, normal.blocks().size());

        PlacementGeometry.Plan close = PlacementGeometry.build(snapshot, target, 2, -64, 320);
        assertTrue(close.blocks().containsKey(new BlockPos(0, 64, 2)));
        assertEquals(1, close.blocks().size());

        PlacementGeometry.Plan tiny = PlacementGeometry.build(snapshot, target, 0.5f, -64, 320);
        assertTrue(tiny.blocks().containsKey(new BlockPos(0, 64, 17)));
        assertEquals(1, tiny.blocks().size());
    }

    @Test
    void cameraZoomMovesCapturedStructureCloserWithoutBreakingDepthAdjacency() {
        WorldSnapshot snapshot = WorldSnapshot.createNew(UUID.randomUUID(), 20, 1, 1, 16, 0, 0, 2);
        snapshot.getOrAddPaletteIndex(Blocks.STONE.defaultBlockState());
        snapshot.getBlocks().add(new CapturedBlock(-0.5f, -0.5f, 10.5f, 0, null));
        snapshot.getBlocks().add(new CapturedBlock(-0.5f, -0.5f, 11.5f, 0, null));

        PlacementGeometry.Plan plan = PlacementGeometry.build(snapshot,
                new CameraTransform(0, 65, 0, 0, 0), 1, -64, 320);
        assertTrue(plan.blocks().containsKey(new BlockPos(0, 64, 5)));
        assertTrue(plan.blocks().containsKey(new BlockPos(0, 64, 6)));
        assertEquals(2, plan.blocks().size());
    }

    @Test
    void chestContentsRemainAttachedToProjectedChest() {
        WorldSnapshot snapshot = WorldSnapshot.createNew(UUID.randomUUID(), 40, 1, 1, 36, 0, 0);
        snapshot.getOrAddPaletteIndex(Blocks.CHEST.defaultBlockState());
        CompoundTag chestData = new CompoundTag();
        chestData.putString("id", "minecraft:chest");
        chestData.putString("TestContents", "retained");
        snapshot.getBlocks().add(new CapturedBlock(-0.5f, -0.5f, 5.5f, 0, chestData));

        PlacementGeometry.Plan plan = PlacementGeometry.build(snapshot,
                new CameraTransform(0, 65, 0, 0, 0), 1, -64, 320);
        assertEquals("retained", plan.blockEntities().get(new BlockPos(0, 64, 5)).getString("TestContents"));
    }

    @Test
    void farPreservesHorizontalAndDepthAdjacency() {
        WorldSnapshot snapshot = WorldSnapshot.createNew(UUID.randomUUID(), 40, 1, 1, 36, 0, 0);
        snapshot.getOrAddPaletteIndex(Blocks.STONE.defaultBlockState());
        snapshot.getBlocks().add(new CapturedBlock(-0.5f, -0.5f, 5.5f, 0, null));
        snapshot.getBlocks().add(new CapturedBlock(0.5f, -0.5f, 5.5f, 0, null));
        snapshot.getBlocks().add(new CapturedBlock(-0.5f, -0.5f, 6.5f, 0, null));
        PlacementGeometry.Plan plan = PlacementGeometry.build(snapshot,
                new CameraTransform(0, 65, 0, 0, 0), 0.5f, -64, 320);
        assertEquals(3, plan.blocks().size());
        assertTrue(plan.blocks().containsKey(new BlockPos(0, 64, 17)));
        assertTrue(plan.blocks().containsKey(new BlockPos(-1, 64, 17)));
        assertTrue(plan.blocks().containsKey(new BlockPos(0, 64, 18)));
    }

    @Test
    void removesBothDoorHalvesWhenProjectionSeparatesThem() {
        Map<BlockPos, net.minecraft.world.level.block.state.BlockState> blocks = new HashMap<>();
        BlockPos lower = new BlockPos(0, 64, 0);
        BlockPos displacedUpper = new BlockPos(0, 65, 1);
        blocks.put(lower, Blocks.OAK_DOOR.defaultBlockState().setValue(
                net.minecraft.world.level.block.DoorBlock.HALF, DoubleBlockHalf.LOWER));
        blocks.put(displacedUpper, Blocks.OAK_DOOR.defaultBlockState().setValue(
                net.minecraft.world.level.block.DoorBlock.HALF, DoubleBlockHalf.UPPER));
        blocks.put(new BlockPos(1, 64, 0), Blocks.STONE.defaultBlockState());

        PlacementGeometry.removeSeparatedMultiblocks(blocks);

        assertEquals(1, blocks.size());
        assertTrue(blocks.containsValue(Blocks.STONE.defaultBlockState()));
    }

    @Test
    void keepsDoorWhenProjectedHalvesRemainVertical() {
        Map<BlockPos, net.minecraft.world.level.block.state.BlockState> blocks = new HashMap<>();
        BlockPos lower = new BlockPos(0, 64, 0);
        BlockPos upper = lower.above();
        blocks.put(lower, Blocks.OAK_DOOR.defaultBlockState().setValue(
                net.minecraft.world.level.block.DoorBlock.HALF, DoubleBlockHalf.LOWER));
        blocks.put(upper, Blocks.OAK_DOOR.defaultBlockState().setValue(
                net.minecraft.world.level.block.DoorBlock.HALF, DoubleBlockHalf.UPPER));

        PlacementGeometry.removeSeparatedMultiblocks(blocks);

        assertEquals(2, blocks.size());
    }

    @Test
    void removesBedPiecesWhenProjectionSeparatesThem() {
        Map<BlockPos, net.minecraft.world.level.block.state.BlockState> blocks = new HashMap<>();
        blocks.put(new BlockPos(0, 64, 0), Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
        blocks.put(new BlockPos(0, 64, 2), Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));

        PlacementGeometry.removeSeparatedMultiblocks(blocks);

        assertTrue(blocks.isEmpty());
    }

    @Test
    void carveMaskClearsInteriorBetweenFloorAndRoofWithoutExpandingOutsideFootprint() {
        Map<BlockPos, net.minecraft.world.level.block.state.BlockState> blocks = new HashMap<>();
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 2; z++) {
                blocks.put(new BlockPos(x, 64, z), Blocks.OAK_PLANKS.defaultBlockState());
                blocks.put(new BlockPos(x, 68, z), Blocks.OAK_PLANKS.defaultBlockState());
            }
        }
        blocks.put(new BlockPos(0, 65, 0), Blocks.OAK_LOG.defaultBlockState());

        var carve = PlacementGeometry.interiorCarvePositions(blocks);

        assertTrue(carve.contains(new BlockPos(1, 65, 1)));
        assertTrue(carve.contains(new BlockPos(1, 67, 1)));
        assertFalse(carve.contains(new BlockPos(0, 65, 0)));
        assertFalse(carve.contains(new BlockPos(1, 64, 1)));
        assertFalse(carve.contains(new BlockPos(3, 65, 1)));
    }
}
