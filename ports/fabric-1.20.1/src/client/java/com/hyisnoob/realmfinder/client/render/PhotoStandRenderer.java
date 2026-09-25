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

    private static final ResourceLocation DEFAULT_PHOTO_TEX = new ResourceLocation("realmfinder", "textures/item/photograph.png");

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
            photoTexture = PhotoCaptureHelper.getTexture(snapshotId, tag);
        }

        poseStack.pushPose();

        // Center the picture inside the model's open frame.
        poseStack.translate(0.5, 0.53125, 0.5);

        // 2. Rotate towards facing direction
        Direction facing = be.getBlockState().getValue(PhotoStandBlock.FACING);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

        // The local +Z face rotates towards the block's FACING direction.
        poseStack.translate(0.0, 0.0, 0.33);

        float photoW = 0.48f;
        float photoH = 0.54f;
        float halfPW = photoW / 2.0f;
        float halfPH = photoH / 2.0f;

        VertexConsumer photoConsumer = bufferSource.getBuffer(RenderType.entityCutout(photoTexture));
        Matrix4f photoMat = poseStack.last().pose();
        Matrix3f photoNormal = poseStack.last().normal();

        // Front face (with photograph texture)
        drawQuad(photoConsumer, photoMat, photoNormal, -halfPW, halfPW, -halfPH, halfPH, 0.002f, 0, 0, 1, packedLight, 0.0f, 1.0f, 0.0f, 1.0f);

        poseStack.popPose();
    }

    private static void drawQuad(VertexConsumer consumer, Matrix4f mat, Matrix3f normal,
                                 float minX, float maxX, float minY, float maxY, float z,
                                 float nx, float ny, float nz, int packedLight,
                                 float u0, float u1, float v0, float v1) {
        consumer.vertex(mat, minX, minY, z)
                .color(255, 255, 255, 255)
                .uv(u0, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, nx, ny, nz).endVertex();

        consumer.vertex(mat, maxX, minY, z)
                .color(255, 255, 255, 255)
                .uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, nx, ny, nz).endVertex();

        consumer.vertex(mat, maxX, maxY, z)
                .color(255, 255, 255, 255)
                .uv(u1, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, nx, ny, nz).endVertex();

        consumer.vertex(mat, minX, maxY, z)
                .color(255, 255, 255, 255)
                .uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, nx, ny, nz).endVertex();
    }
}
