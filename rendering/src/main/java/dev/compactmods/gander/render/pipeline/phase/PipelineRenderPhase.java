package dev.compactmods.gander.render.pipeline.phase;

import dev.compactmods.gander.render.pipeline.PipelineState;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

@FunctionalInterface
public interface PipelineRenderPhase extends PipelinePhase {

    void render(PipelineState state, GuiGraphics graphics, Camera camera, Matrix4f projectionMatrix);

}
