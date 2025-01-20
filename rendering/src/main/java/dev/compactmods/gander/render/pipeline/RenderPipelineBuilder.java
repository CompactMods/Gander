package dev.compactmods.gander.render.pipeline;

import dev.compactmods.gander.render.pipeline.phase.IPipelinePhaseCollectionBuilder;
import dev.compactmods.gander.render.pipeline.phase.PipelinePhaseCollection;

public class RenderPipelineBuilder<TCtx> {

    private final PipelinePhaseCollection.Builder<TCtx> phaseCollectionBuilder;

    public RenderPipelineBuilder() {
        this.phaseCollectionBuilder = new PipelinePhaseCollection.Builder<>();
    }

    public IPipelinePhaseCollectionBuilder<TCtx> phases() {
        return phaseCollectionBuilder;
    }

    public SinglePassRenderPipeline<TCtx> singlePass() {
        return new SingleEntrypointRenderPipeline<>(phaseCollectionBuilder.build());
    }
}
