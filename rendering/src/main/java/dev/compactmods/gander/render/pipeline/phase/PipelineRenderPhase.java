package dev.compactmods.gander.render.pipeline.phase;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.PipelineState;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

@FunctionalInterface
public interface PipelineRenderPhase<TCtx> extends PipelinePhase {

    void render(PipelineState state, TCtx context, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix);

}
