package dev.compactmods.gander.render;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.context.LevelRenderingContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3fc;

/**
 * A render pipeline is a pre-configured set of rendering steps for rendering a
 * level to a specific target.
 */
public interface RenderPipeline<TLevel extends Level, TCtx extends LevelRenderingContext<TLevel>> {

    default void staticGeometryPass(TCtx ctx, float partialTick, RenderType renderType, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix) {
        staticGeometryPass(ctx, partialTick, renderType, poseStack, camera, projectionMatrix, Vec3.ZERO.toVector3f());
    }

    void staticGeometryPass(TCtx ctx, float partialTick, RenderType renderType, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc renderOffset);

    default void blockEntitiesPass(TCtx ctx, float partialTick, PoseStack poseStack, Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource) {
        blockEntitiesPass(ctx, partialTick, poseStack, camera, frustum, bufferSource, Vec3.ZERO.toVector3f());
    }

    void blockEntitiesPass(TCtx ctx, float partialTick, PoseStack poseStack, Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource, Vector3fc renderOffset);
}
