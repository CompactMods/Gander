package dev.compactmods.gander.render.pipeline.phase;

import dev.compactmods.gander.render.pipeline.PipelineState;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.RenderType;

public interface PipelineGeometryUploadPhase extends PipelinePhase {

    default boolean shouldRun(RenderType renderType) {
        return true;
    }

    void upload(PipelineState state, GuiGraphics graphics);
}
