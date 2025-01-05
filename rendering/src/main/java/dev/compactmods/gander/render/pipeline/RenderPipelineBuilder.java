package dev.compactmods.gander.render.pipeline;

import dev.compactmods.gander.render.pipeline.context.LevelRenderingContext;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class RenderPipelineBuilder<TCtx extends LevelRenderingContext> {

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

    public RenderPipeline<TCtx> build() {
        return new SingleEntrypointRenderPipeline<TCtx>(setupPhases, cleanupPhases, geometryPhases);
    }
}
