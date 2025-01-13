package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.RenderType;

import net.minecraft.client.renderer.culling.Frustum;

import org.joml.Matrix4f;

public interface MultiPassRenderPipeline<TCtx> extends RenderPipeline<TCtx> {

    void renderPass(PipelineState state, TCtx ctx, RenderType renderType, GuiGraphics graphics, Camera camera, Frustum frustum, PoseStack poseStack, Matrix4f projectionMatrix, Matrix4f modelViewMatrix);

}
