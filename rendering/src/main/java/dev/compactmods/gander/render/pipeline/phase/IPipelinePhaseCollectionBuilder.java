package dev.compactmods.gander.render.pipeline.phase;

import net.minecraft.client.renderer.RenderType;

import java.util.function.Predicate;

public interface IPipelinePhaseCollectionBuilder {
    IPipelinePhaseCollectionBuilder addSetupPhase(PipelineLifecyclePhase phase);

    IPipelinePhaseCollectionBuilder addGeometryUploadPhase(PipelineGeometryUploadPhase phase);

    IPipelinePhaseCollectionBuilder addGeometryUploadPhase(Predicate<RenderType> predicate, PipelineGeometryUploadPhase phase);

    /**
     * Called before all geometry upload phases.
     *
     * @param phase
     * @return
     */
    IPipelinePhaseCollectionBuilder addPreGeometryPhase(PipelineLifecyclePhase phase);

    IPipelinePhaseCollectionBuilder addRenderPhase(PipelineRenderPhase phase);

    IPipelinePhaseCollectionBuilder addCleanupPhase(PipelineLifecyclePhase phase);
}
