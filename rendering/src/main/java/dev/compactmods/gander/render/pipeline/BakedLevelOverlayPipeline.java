package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.RenderPipeline;
import dev.compactmods.gander.render.pipeline.context.BakedDirectLevelRenderingContext;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.function.Function;

public final class BakedLevelOverlayPipeline<T extends Level> implements RenderPipeline<T> {

    @Override
    public void staticGeometryPass(BakedDirectLevelRenderingContext<T> ctx, RenderType renderType, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc renderOffset) {
        poseStack.pushPose();
        poseStack.translate(renderOffset.x(), renderOffset.y(), renderOffset.z());

        BlockRenderer.renderSectionLayer(
            ctx.blockBuffers(),
            Function.identity(),
            renderType,
            poseStack,
            camera.getPosition().toVector3f(),
            projectionMatrix);
        BlockRenderer.renderSectionLayer(
            ctx.fluidBuffers(),
            Function.identity(),
            renderType,
            poseStack,
            camera.getPosition().toVector3f(),
            projectionMatrix);
        poseStack.popPose();
    }

    @Override
    public void blockEntitiesPass(BakedDirectLevelRenderingContext<T> ctx, float partialTick, PoseStack poseStack, Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource, Vector3fc renderOffset) {
        final var camPos = camera.getPosition();

        final var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();

        poseStack.pushPose();
        poseStack.translate(renderOffset.x() - camPos.x, renderOffset.y() - camPos.y, renderOffset.z() - camPos.z);
        ctx.blockEntities().get()
            .forEach(blockEnt -> {
                if (!isBlockEntityRendererVisible(dispatcher, blockEnt, frustum, renderOffset)) return;

                poseStack.pushPose();
                final var offset = Vec3.atLowerCornerOf(blockEnt.getBlockPos());
                poseStack.translate(offset.x, offset.y, offset.z);
                dispatcher.render(blockEnt, partialTick, poseStack, bufferSource);
                poseStack.popPose();
            });
        poseStack.popPose();
    }

    private boolean isBlockEntityRendererVisible(BlockEntityRenderDispatcher dispatcher, BlockEntity blockEntity, Frustum frustum, Vector3fc renderOffset) {
        var renderer = dispatcher.getRenderer(blockEntity);
        return renderer != null && frustum.isVisible(renderer.getRenderBoundingBox(blockEntity).move(renderOffset.x(), renderOffset.y(), renderOffset.z()));
    }
}
