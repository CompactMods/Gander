package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

public interface PipelineGeometryUploadPhase<TCtx> {

    void upload(PipelineState state, TCtx context, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix);
}
