package dev.compactmods.gander.render.pipeline;

import dev.compactmods.gander.render.pipeline.phase.IPipelinePhaseCollectionBuilder;
import dev.compactmods.gander.render.pipeline.phase.PipelinePhaseCollection;

public class RenderPipelineBuilder {

    private final PipelinePhaseCollection.Builder phaseCollectionBuilder;

    public RenderPipelineBuilder() {
        this.phaseCollectionBuilder = new PipelinePhaseCollection.Builder();
    }

    public IPipelinePhaseCollectionBuilder phases() {
        return phaseCollectionBuilder;
    }

    public SinglePassRenderPipeline singlePass() {
        return new SingleEntrypointRenderPipeline(phaseCollectionBuilder.build());
    }

    public MultiPassRenderPipeline stagedMultiPass() {
        return new StagedMultipassRenderPipeline(phaseCollectionBuilder.build());
    }
}
