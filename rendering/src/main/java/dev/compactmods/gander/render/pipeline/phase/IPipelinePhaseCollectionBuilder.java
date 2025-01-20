package dev.compactmods.gander.render.pipeline.phase;

public interface IPipelinePhaseCollectionBuilder<TCtx> {
    IPipelinePhaseCollectionBuilder<TCtx> addSetupPhase(PipelineLifecyclePhase phase);

    IPipelinePhaseCollectionBuilder<TCtx> addContextSetupPhase(ContextAwareSetupPhase<TCtx> phase);

    IPipelinePhaseCollectionBuilder<TCtx> addGeometryUploadPhase(PipelineGeometryUploadPhase<TCtx> phase);

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
