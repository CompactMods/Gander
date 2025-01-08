package dev.compactmods.gander.render.pipeline;

import java.util.LinkedHashSet;
import java.util.Set;

public class RenderPipelineBuilder<TCtx> {

    private final Set<PipelineLifecyclePhase<TCtx>> setupPhases = new LinkedHashSet<>();
    private final Set<PipelineLifecyclePhase<TCtx>> cleanupPhases = new LinkedHashSet<>();
    private final Set<PipelineGeometryUploadPhase<TCtx>> geometryPhases = new LinkedHashSet<>();

    public RenderPipelineBuilder<TCtx> addSetupPhase(PipelineLifecyclePhase<TCtx> phase) {
        this.setupPhases.add(phase);
        return this;
    }

    public RenderPipelineBuilder<TCtx> addGeometryUploadPhase(PipelineGeometryUploadPhase<TCtx> phase) {
        this.geometryPhases.add(phase);
        return this;
    }

    public RenderPipelineBuilder<TCtx> addCleanupPhase(PipelineLifecyclePhase<TCtx> phase) {
        this.cleanupPhases.add(phase);
        return this;
    }

    public SinglePassRenderPipeline<TCtx> singlePass() {
        return new SingleEntrypointRenderPipeline<TCtx>(setupPhases, cleanupPhases, geometryPhases);
    }
}
