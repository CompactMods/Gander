package dev.compactmods.gander.render.pipeline;

import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

public interface SinglePassRenderPipeline extends RenderPipeline {

    void render(PipelineState state, GuiGraphics graphics, Camera camera, Matrix4f projectionMatrix, Matrix4f viewMatrix, float partialTicks);

}
