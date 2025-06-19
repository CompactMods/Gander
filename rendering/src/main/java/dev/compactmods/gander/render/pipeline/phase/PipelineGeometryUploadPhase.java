package dev.compactmods.gander.render.pipeline.phase;

import dev.compactmods.gander.render.pipeline.PipelineState;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;

public interface PipelineGeometryUploadPhase extends PipelinePhase {

    default boolean shouldRun(ChunkSectionLayerGroup layer) {
        return true;
    }

    void upload(PipelineState state);
}
