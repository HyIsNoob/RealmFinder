package com.hyisnoob.realmfinder.client.render;

import com.hyisnoob.realmfinder.client.render.PhotoCaptureHelper;
import com.hyisnoob.realmfinder.common.block.PhotoStandBlock;
import com.hyisnoob.realmfinder.common.block.PhotoStandBlockEntity;
import com.hyisnoob.realmfinder.core.service.PlatformHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.UUID;

public class PhotoStandRenderer implements BlockEntityRenderer<PhotoStandBlockEntity> {

    private static final ResourceLocation DEFAULT_PHOTO_TEX = ResourceLocation.fromNamespaceAndPath("realmfinder", "textures/item/photograph.png");
    private static final ResourceLocation BACKING_TEX = ResourceLocation.withDefaultNamespace("textures/block/spruce_planks.png");

    public PhotoStandRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PhotoStandBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!be.hasPhoto()) {
            return;
        }

        ItemStack photoStack = be.getPhoto();
        ResourceLocation photoTexture = DEFAULT_PHOTO_TEX;

        CompoundTag tag = PlatformHelper.getCustomTag(photoStack);
        if (tag.hasUUID("SnapshotId")) {
            UUID snapshotId = tag.getUUID("SnapshotId");
            photoTexture = PhotoCaptureHelper.getTexture(snapshotId);
        }

        poseStack.pushPose();

        // 1. Center of the stand block
        poseStack.translate(0.5, 0.48, 0.5);

        // 2. Rotate towards facing direction
        Direction facing = be.getBlockState().getValue(PhotoStandBlock.FACING);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

        // 3. Move forward slightly onto the easel shelf ledge and tilt back
        poseStack.translate(0.0, 0.0, -0.19);
        poseStack.mulPose(Axis.XP.rotationDegrees(12.0f));

        // 4. Render Backing Plate (Wooden board support)
        float boardW = 0.68f;
        float boardH = 0.68f;
        float halfBW = boardW / 2.0f;
        float halfBH = boardH / 2.0f;

        VertexConsumer boardConsumer = bufferSource.getBuffer(RenderType.entityCutout(BACKING_TEX));
        Matrix4f boardMat = poseStack.last().pose();
        Matrix3f boardNormal = poseStack.last().normal();

        // Backing back-face
        drawQuad(boardConsumer, boardMat, boardNormal, halfBW, -halfBW, -halfBH, halfBH, -0.005f, 0, 0, -1, packedLight, 0.0f, 1.0f, 0.0f, 1.0f);

        // 5. Render Front Photograph Quad
        float photoW = 0.62f;
        float photoH = 0.62f;
        float halfPW = photoW / 2.0f;
        float halfPH = photoH / 2.0f;

        VertexConsumer photoConsumer = bufferSource.getBuffer(RenderType.entityCutout(photoTexture));
        Matrix4f photoMat = poseStack.last().pose();
        Matrix3f photoNormal = poseStack.last().normal();

        // Front face (with photograph texture)
        drawQuad(photoConsumer, photoMat, photoNormal, -halfPW, halfPW, -halfPH, halfPH, 0.002f, 0, 0, 1, packedLight, 0.0f, 1.0f, 1.0f, 0.0f);

        poseStack.popPose();
    }

    private static void drawQuad(VertexConsumer consumer, Matrix4f mat, Matrix3f normal,
                                 float minX, float maxX, float minY, float maxY, float z,
                                 float nx, float ny, float nz, int packedLight,
                                 float u0, float u1, float v0, float v1) {
        consumer.addVertex(mat, minX, minY, z)
                .setColor(255, 255, 255, 255)
                .setUv(u0, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(nx, ny, nz);

        consumer.addVertex(mat, maxX, minY, z)
                .setColor(255, 255, 255, 255)
                .setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(nx, ny, nz);

        consumer.addVertex(mat, maxX, maxY, z)
                .setColor(255, 255, 255, 255)
                .setUv(u1, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(nx, ny, nz);

        consumer.addVertex(mat, minX, maxY, z)
                .setColor(255, 255, 255, 255)
                .setUv(u0, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(nx, ny, nz);
    }
}
