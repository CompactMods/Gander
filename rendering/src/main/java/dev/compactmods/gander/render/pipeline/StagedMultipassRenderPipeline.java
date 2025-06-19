package dev.compactmods.gander.render.pipeline;

import dev.compactmods.gander.render.pipeline.phase.PipelinePhaseCollection;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.culling.Frustum;

import java.util.function.Consumer;

public record StagedMultipassRenderPipeline(PipelinePhaseCollection phaseCollection) implements MultiPassRenderPipeline {

    @Override
    public void renderPass(PipelineState state, ChunkSectionLayerGroup layerGroup, Frustum frustum) {
        for (var preRenderPhase : phaseCollection.beforeGeometryPhases())
            preRenderPhase.run(state);

        for (var phase : phaseCollection.geometryUploadPhases()) {
            if(phase.shouldRun(layerGroup))
                phase.upload(state);
        }

        for (var phase : phaseCollection.renderPhases())
            phase.render(state);

        for (var phase : phaseCollection.cleanupPhases())
            phase.run(state);
    }

    @Override
    public PipelineState setup(Consumer<PipelineState> initializer) {
        final var state = new PipelineState();
        initializer.accept(state);
        return state;
    }
}
