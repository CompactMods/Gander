package dev.compactmods.gander.render.pipeline.example;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.core.camera.MovableCamera;
import dev.compactmods.gander.render.pipeline.context.BakedDirectLevelRenderingContext;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Set;
import java.util.function.Function;

public final class BakedLevelOverlayPipeline { // implements RenderPipeline<BakedDirectLevelRenderingContext> {

    private final Set<RenderType> STATIC_GEOMETRY = Set.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout());

    private final MovableCamera movableCamera = new MovableCamera();

    // @Override
    public void staticGeometryPass(BakedDirectLevelRenderingContext ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc origin) {
        // Rebase the camera so that blocks get coordinates relative to their inner level, rather than the real level
        movableCamera.setup(camera.getEntity().level(), camera.getEntity(), camera.isDetached(), false, partialTick);
        movableCamera.moveWorldSpace(-origin.x(), -origin.y(), -origin.z());

        poseStack.pushPose();

        STATIC_GEOMETRY.forEach(renderType -> {
            BlockRenderer.renderSectionLayer(
                ctx.blockBuffers(),
                Function.identity(),
                renderType,
                poseStack,
                movableCamera.getPosition().toVector3f(),
                projectionMatrix);

            BlockRenderer.renderSectionLayer(
                ctx.fluidBuffers(),
                Function.identity(),
                renderType,
                poseStack,
                movableCamera.getPosition().toVector3f(),
                projectionMatrix);
        });

        poseStack.popPose();
    }

    // @Override
    public void blockEntitiesPass(BakedDirectLevelRenderingContext ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource, Vector3fc origin) {
        // Rebase the camera so that block entities get coordinates relative to their inner level, rather than the real level
        movableCamera.setup(camera.getEntity().level(), camera.getEntity(), camera.isDetached(), false, partialTick);
        movableCamera.moveWorldSpace(-origin.x(), -origin.y(), -origin.z());

        final var mc = Minecraft.getInstance();
        final var blockEntityRenderDispatcher = mc.getBlockEntityRenderDispatcher();

        // TODO: maybe we should raycast in the virtual level for these, rather than pulling from the real level?
        mc.getEntityRenderDispatcher().prepare(camera.getEntity().level(), movableCamera, Minecraft.getInstance().crosshairPickEntity);
        blockEntityRenderDispatcher.prepare(camera.getEntity().level(), movableCamera, Minecraft.getInstance().hitResult);

        final var camPos = camera.getPosition();
        final var renderOffset = new Vector3f(
            (float) (origin.x() - camPos.x),
            (float) (origin.y() - camPos.y),
            (float) (origin.z() - camPos.z));

        poseStack.pushPose();
        poseStack.translate(renderOffset.x, renderOffset.y, renderOffset.z);
        ctx.blockEntities().get()
            .forEach(blockEnt -> {
                if (!isBlockEntityRendererVisible(blockEntityRenderDispatcher, blockEnt, frustum, origin)) return;

                poseStack.pushPose();
                final var offset = Vec3.atLowerCornerOf(blockEnt.getBlockPos());
                poseStack.translate(offset.x, offset.y, offset.z);
                blockEntityRenderDispatcher.render(blockEnt, partialTick, poseStack, bufferSource);
                poseStack.popPose();
            });
        poseStack.popPose();
    }

    private boolean isBlockEntityRendererVisible(BlockEntityRenderDispatcher dispatcher, BlockEntity blockEntity, Frustum frustum, Vector3fc origin) {
        var renderer = dispatcher.getRenderer(blockEntity);
        return renderer != null && frustum.isVisible(renderer.getRenderBoundingBox(blockEntity).move(origin.x(), origin.y(), origin.z()));
    }

    // @Override
    public void translucentGeometryPass(BakedDirectLevelRenderingContext ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc renderOffset) {
        BlockRenderer.renderSectionLayer(
            ctx.fluidBuffers(),
            Function.identity(),
            RenderType.translucent(),
            poseStack,
            movableCamera.getPosition().toVector3f(),
            projectionMatrix);

        BlockRenderer.renderSectionLayer(
            ctx.blockBuffers(),
            Function.identity(),
            RenderType.translucent(),
            poseStack,
            movableCamera.getPosition().toVector3f(),
            projectionMatrix);
    }
}
