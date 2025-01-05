package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.context.LevelRenderingContext;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;

import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A render pipeline is a pre-configured set of rendering steps for rendering a
 * level to a specific target.
 */
public interface RenderPipeline<TCtx extends LevelRenderingContext> {

    default void staticGeometryPass(TCtx ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix) {
        staticGeometryPass(ctx, graphics, partialTick, poseStack, camera, projectionMatrix, Vec3.ZERO.toVector3f());
    }

    void staticGeometryPass(TCtx ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc renderOffset);

    default void blockEntitiesPass(TCtx ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource) {
        blockEntitiesPass(ctx, graphics, partialTick, poseStack, camera, frustum, bufferSource, Vec3.ZERO.toVector3f());
    }

    void blockEntitiesPass(TCtx ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource, Vector3fc renderOffset);

    default void translucentGeometryPass(TCtx ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix) {
        translucentGeometryPass(ctx, graphics, partialTick, poseStack, camera, projectionMatrix, new Vector3f());
    }

    void translucentGeometryPass(TCtx ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc renderOffset);
}
