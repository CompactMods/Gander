package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.context.LevelRenderingContext;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
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

    PipelineState setup(TCtx ctx, GuiGraphics graphics, Camera camera);

    void render(PipelineState state, TCtx ctx, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix);

}
