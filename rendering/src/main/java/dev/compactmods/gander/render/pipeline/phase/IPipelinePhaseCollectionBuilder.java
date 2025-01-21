package dev.compactmods.gander.render.pipeline.phase;

import net.minecraft.client.renderer.RenderType;

import java.util.function.Predicate;

public interface IPipelinePhaseCollectionBuilder<TCtx> {
    IPipelinePhaseCollectionBuilder<TCtx> addSetupPhase(PipelineLifecyclePhase phase);

    IPipelinePhaseCollectionBuilder<TCtx> addContextSetupPhase(ContextAwareSetupPhase<TCtx> phase);

    IPipelinePhaseCollectionBuilder<TCtx> addGeometryUploadPhase(PipelineGeometryUploadPhase<TCtx> phase);

    IPipelinePhaseCollectionBuilder<TCtx> addGeometryUploadPhase(Predicate<RenderType> predicate, PipelineGeometryUploadPhase<TCtx> phase);

    /**
     * Called before all geometry upload phases.
     *
     * @param phase
     * @return
     */
    IPipelinePhaseCollectionBuilder<TCtx> addPreGeometryPhase(PipelineLifecyclePhase phase);

    IPipelinePhaseCollectionBuilder<TCtx> addRenderPhase(PipelineRenderPhase<TCtx> phase);

    IPipelinePhaseCollectionBuilder<TCtx> addCleanupPhase(PipelineLifecyclePhase phase);
}
