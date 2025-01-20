package dev.compactmods.gander.render.pipeline.phase;

import java.util.LinkedHashSet;
import java.util.Set;

public record PipelinePhaseCollection<TCtx>(
    Set<PipelineLifecyclePhase> setupPhases,
    Set<ContextAwareSetupPhase<TCtx>> contextSetupPhases,
    Set<PipelineLifecyclePhase> beforeGeometryPhases,
    Set<PipelineGeometryUploadPhase<TCtx>> geometryUploadPhases,
    Set<PipelineRenderPhase<TCtx>> renderPhases,
    Set<PipelineLifecyclePhase> cleanupPhases
) {


    public static class Builder<TCtx> implements IPipelinePhaseCollectionBuilder<TCtx> {


        private final Set<PipelineLifecyclePhase> setupPhases = new LinkedHashSet<>();
        private final Set<ContextAwareSetupPhase<TCtx>> contextSetupPhases = new LinkedHashSet<>();
        private final Set<PipelineLifecyclePhase> cleanupPhases = new LinkedHashSet<>();
        private final Set<PipelineLifecyclePhase> beforeGeometryPhases = new LinkedHashSet<>();
        private final Set<PipelineGeometryUploadPhase<TCtx>> geometryPhases = new LinkedHashSet<>();
        private final Set<PipelineRenderPhase<TCtx>> renderPhases = new LinkedHashSet<>();

        public Builder<TCtx> addSetupPhase(PipelineLifecyclePhase phase) {
            this.setupPhases.add(phase);
            return this;
        }

        public Builder<TCtx> addContextSetupPhase(ContextAwareSetupPhase<TCtx> phase) {
            this.contextSetupPhases.add(phase);
            return this;
        }

        public Builder<TCtx> addGeometryUploadPhase(PipelineGeometryUploadPhase<TCtx> phase) {
            this.geometryPhases.add(phase);
            return this;
        }

        /**
         * Called before all geometry upload phases.
         *
         * @param phase
         * @return
         */
        public Builder<TCtx> addPreGeometryPhase(PipelineLifecyclePhase phase) {
            this.beforeGeometryPhases.add(phase);
            return this;
        }

        public Builder<TCtx> addRenderPhase(PipelineRenderPhase<TCtx> phase) {
            this.renderPhases.add(phase);
            return this;
        }

        public Builder<TCtx> addCleanupPhase(PipelineLifecyclePhase phase) {
            this.cleanupPhases.add(phase);
            return this;
        }

        public PipelinePhaseCollection<TCtx> build() {
            return new PipelinePhaseCollection<>(setupPhases, contextSetupPhases, beforeGeometryPhases, geometryPhases, renderPhases, cleanupPhases);
        }
    }
}
