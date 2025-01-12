package dev.compactmods.gander.render.pipeline;

import dev.compactmods.gander.render.pipeline.phase.ContextAwareSetupPhase;
import dev.compactmods.gander.render.pipeline.phase.PipelineGeometryUploadPhase;
import dev.compactmods.gander.render.pipeline.phase.PipelineLifecyclePhase;
import dev.compactmods.gander.render.pipeline.phase.PipelineRenderPhase;

import java.util.LinkedHashSet;
import java.util.Set;

public class RenderPipelineBuilder<TCtx> {

    private final Set<PipelineLifecyclePhase> setupPhases = new LinkedHashSet<>();
    private final Set<ContextAwareSetupPhase<TCtx>> contextSetupPhases = new LinkedHashSet<>();
    private final Set<PipelineLifecyclePhase> cleanupPhases = new LinkedHashSet<>();
    private final Set<PipelineGeometryUploadPhase<TCtx>> geometryPhases = new LinkedHashSet<>();
    private final Set<PipelineRenderPhase<TCtx>> renderPhases = new LinkedHashSet<>();

    public RenderPipelineBuilder<TCtx> addSetupPhase(PipelineLifecyclePhase phase) {
        this.setupPhases.add(phase);
        return this;
    }

    public RenderPipelineBuilder<TCtx> addContextSetupPhase(ContextAwareSetupPhase<TCtx> phase) {
        this.contextSetupPhases.add(phase);
        return this;
    }

    public RenderPipelineBuilder<TCtx> addGeometryUploadPhase(PipelineGeometryUploadPhase<TCtx> phase) {
        this.geometryPhases.add(phase);
        return this;
    }

    public RenderPipelineBuilder<TCtx> addRenderPhase(PipelineRenderPhase<TCtx> phase) {
        this.renderPhases.add(phase);
        return this;
    }

    public RenderPipelineBuilder<TCtx> addCleanupPhase(PipelineLifecyclePhase phase) {
        this.cleanupPhases.add(phase);
        return this;
    }

    public SinglePassRenderPipeline<TCtx> singlePass() {
        return new SingleEntrypointRenderPipeline<TCtx>(setupPhases, contextSetupPhases, cleanupPhases, geometryPhases, renderPhases);
    }
}
