package com.hyisnoob.realmfinder.client.render;

import com.hyisnoob.realmfinder.RealmFinder;
import com.hyisnoob.realmfinder.common.item.ModItems;
import com.hyisnoob.realmfinder.core.engine.GameplayLimits;
import com.hyisnoob.realmfinder.core.engine.PlacementGeometry;
import com.hyisnoob.realmfinder.core.math.CameraTransform;
import com.hyisnoob.realmfinder.core.service.PlatformHelper;
import com.hyisnoob.realmfinder.core.snapshot.SnapshotSerializer;
import com.hyisnoob.realmfinder.core.snapshot.CapturedEntity;
import com.hyisnoob.realmfinder.core.snapshot.WorldSnapshot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import org.joml.Vector3d;

public final class FrustumGhostRenderer {
    private static final int EXACT_OUTLINE_LIMIT = 2048;
    private static UUID cachedId;
    private static ClientLevel cachedLevel;
    private static WorldSnapshot cachedSnapshot;
    private static PlacementGeometry.Plan cachedPlan;
    private static long cachedTick = Long.MIN_VALUE;
    private static float cachedScale;
    private static boolean cachedCarve;
    private static boolean cachedValid;

    private FrustumGhostRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || !mc.options.getCameraType().isFirstPerson()
                    || context.matrixStack() == null || context.consumers() == null
                    || PhotoCaptureHelper.isPendingCapture()) return;
            ItemStack main = mc.player.getMainHandItem();
            ItemStack off = mc.player.getOffhandItem();
            if (main.is(ModItems.CAMERA) || off.is(ModItems.CAMERA)) return;
            ItemStack photo = main.is(ModItems.PHOTOGRAPH) ? main : off.is(ModItems.PHOTOGRAPH) ? off : ItemStack.EMPTY;
            if (photo.isEmpty()) {
                cachedPlan = null;
                return;
            }
            if (cachedLevel != mc.level) {
                cachedLevel = mc.level;
                cachedTick = Long.MIN_VALUE;
            }
            CompoundTag tag = PlatformHelper.getCustomTag(photo);
            if (!tag.hasUUID("SnapshotId")) return;
            UUID id = tag.getUUID("SnapshotId");
            if (!id.equals(cachedId)) {
                cachedId = id;
                try {
                    cachedSnapshot = SnapshotSerializer.fromNbt(tag);
                } catch (RuntimeException e) {
                    RealmFinder.LOGGER.warn("Could not preview malformed Photograph {}", id, e);
                    cachedSnapshot = null;
                }
                cachedTick = Long.MIN_VALUE;
            }
            if (cachedSnapshot == null) return;

            long tick = mc.level.getGameTime();
            float scale = ViewfinderOverlayRenderer.getCurrentScale();
            boolean carve = !mc.player.isShiftKeyDown();
            if (tick != cachedTick || scale != cachedScale || carve != cachedCarve) {
                cachedTick = tick;
                cachedScale = scale;
                cachedCarve = carve;
                CameraTransform camera = new CameraTransform(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(),
                        mc.player.getYRot(), mc.player.getXRot());
                cachedPlan = PlacementGeometry.build(cachedSnapshot, camera, scale,
                        mc.level.getMinBuildHeight(), mc.level.getMaxBuildHeight());
                cachedValid = validate(mc, cachedPlan, carve);
            }
            if (cachedPlan == null || (cachedPlan.blocks().isEmpty() && cachedSnapshot.getEntities().isEmpty())) return;

            PoseStack pose = context.matrixStack();
            Vec3 cameraPos = context.camera().getPosition();
            pose.pushPose();
            pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            VertexConsumer lines = context.consumers().getBuffer(RenderType.lines());
            float red = cachedValid ? 0.1f : 1.0f;
            float green = cachedValid ? 0.95f : 0.2f;
            float blue = cachedValid ? 0.65f : 0.2f;
            if (cachedPlan.blocks().size() <= EXACT_OUTLINE_LIMIT) {
                for (BlockPos pos : cachedPlan.blocks().keySet()) {
                    LevelRenderer.renderLineBox(pose, lines, new AABB(pos), red, green, blue, 0.55f);
                }
            } else {
                LevelRenderer.renderLineBox(pose, lines,
                        new AABB(cachedPlan.minX(), cachedPlan.minY(), cachedPlan.minZ(),
                                cachedPlan.maxX() + 1, cachedPlan.maxY() + 1, cachedPlan.maxZ() + 1),
                        red, green, blue, 0.8f);
            }
            for (CapturedEntity captured : cachedSnapshot.getEntities()) {
                Vector3d world = PlacementGeometry.worldPosition(cachedPlan.camera(),
                        captured.getCamX(), captured.getCamY(), captured.getCamZ(), scale,
                        cachedSnapshot.getCaptureZoom(), cachedSnapshot.getFarPlane());
                LevelRenderer.renderLineBox(pose, lines,
                        new AABB(world.x - 0.4, world.y, world.z - 0.4,
                                world.x + 0.4, world.y + 1.6, world.z + 0.4),
                        red, green, blue, 0.8f);
            }
            pose.popPose();
        });
    }

    private static boolean validate(Minecraft mc, PlacementGeometry.Plan plan, boolean carve) {
        if (plan == null || (plan.blocks().isEmpty() && cachedSnapshot.getEntities().isEmpty())) return false;
        if (plan.blocks().size() > GameplayLimits.MAX_PLACED_BLOCKS) return false;
        for (BlockPos pos : plan.blocks().keySet()) {
            if (!mc.level.hasChunkAt(pos) || !mc.level.getWorldBorder().isWithinBounds(pos)
                    || mc.level.getBlockEntity(pos) != null || mc.level.getBlockState(pos).is(Blocks.BEDROCK)) return false;
        }
        if (com.hyisnoob.realmfinder.client.ServerSettings.get().mayCarve(carve, cachedScale) && !plan.blocks().isEmpty()) {
            if ((long) plan.blocks().size() + plan.carvePositions().size() > GameplayLimits.MAX_PLACED_BLOCKS) return false;
            for (BlockPos pos : plan.carvePositions()) {
                if (!mc.level.hasChunkAt(pos) || !mc.level.getWorldBorder().isWithinBounds(pos)
                        || mc.level.getBlockEntity(pos) != null) return false;
            }
        }
        for (CapturedEntity captured : cachedSnapshot.getEntities()) {
            Vector3d world = PlacementGeometry.worldPosition(plan.camera(),
                    captured.getCamX(), captured.getCamY(), captured.getCamZ(), cachedScale,
                    cachedSnapshot.getCaptureZoom(), cachedSnapshot.getFarPlane());
            if (!Double.isFinite(world.x) || !Double.isFinite(world.y) || !Double.isFinite(world.z)) return false;
            BlockPos pos = BlockPos.containing(world.x, world.y, world.z);
            if (mc.level.isOutsideBuildHeight(pos) || !mc.level.hasChunkAt(pos)
                    || !mc.level.getWorldBorder().isWithinBounds(pos)) return false;
        }
        return true;
    }

    public static String getStatusText() {
        if (cachedPlan == null) return "TARGET: NO PREVIEW";
        if (!cachedValid) return "TARGET: BLOCKED";
        if (cachedPlan.blocks().size() > EXACT_OUTLINE_LIMIT) return "TARGET: OUTLINE";
        return "TARGET: " + cachedPlan.blocks().size() + " BLKS";
    }

    public static boolean isValid() {
        return cachedValid;
    }
}
