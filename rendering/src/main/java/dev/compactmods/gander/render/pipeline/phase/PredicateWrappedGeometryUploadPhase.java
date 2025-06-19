package dev.compactmods.gander.render.pipeline.phase;

import dev.compactmods.gander.render.pipeline.PipelineState;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;

import java.util.function.Predicate;

public record PredicateWrappedGeometryUploadPhase(Predicate<ChunkSectionLayerGroup> predicate, PipelineGeometryUploadPhase phase) implements PipelineGeometryUploadPhase {

    @Override
    public boolean shouldRun(ChunkSectionLayerGroup layer) {
        if(predicate == null) return true;
        return predicate.test(layer);
    }

    @Override
    public void upload(PipelineState state) {
        phase.upload(state);
    }
}
