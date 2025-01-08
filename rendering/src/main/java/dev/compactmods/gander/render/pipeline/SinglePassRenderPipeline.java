package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

public interface SinglePassRenderPipeline<TCtx> extends RenderPipeline<TCtx> {

    void render(PipelineState state, TCtx ctx, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix);

}
