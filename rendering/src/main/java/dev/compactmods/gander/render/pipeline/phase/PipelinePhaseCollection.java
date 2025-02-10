package dev.compactmods.gander.render.pipeline.phase;

import net.minecraft.client.renderer.RenderType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

public record PipelinePhaseCollection(
    Set<PipelineLifecyclePhase> setupPhases,
    Set<PipelineLifecyclePhase> beforeGeometryPhases,
    Set<PipelineGeometryUploadPhase> geometryUploadPhases,
    Set<PipelineRenderPhase> renderPhases,
    Set<PipelineLifecyclePhase> cleanupPhases
) {

    public static class Builder implements IPipelinePhaseCollectionBuilder {

        private final Set<PipelineLifecyclePhase> setupPhases = new LinkedHashSet<>();
        private final Set<PipelineLifecyclePhase> cleanupPhases = new LinkedHashSet<>();
        private final Set<PipelineLifecyclePhase> beforeGeometryPhases = new LinkedHashSet<>();
        private final Set<PipelineGeometryUploadPhase> geometryPhases = new LinkedHashSet<>();
        private final Set<PipelineRenderPhase> renderPhases = new LinkedHashSet<>();

        public Builder addSetupPhase(PipelineLifecyclePhase phase) {
            this.setupPhases.add(phase);
            return this;
        }

        public Builder addGeometryUploadPhase(PipelineGeometryUploadPhase phase) {
            this.geometryPhases.add(phase);
            return this;
        }

        @Override
        public IPipelinePhaseCollectionBuilder addGeometryUploadPhase(Predicate<RenderType> predicate, PipelineGeometryUploadPhase phase) {
            this.geometryPhases.add(new PredicateWrappedGeometryUploadPhase(predicate, phase));
            return this;
        }

        /**
         * Called before all geometry upload phases.
         *
         * @param phase
         * @return
         */
        public Builder addPreGeometryPhase(PipelineLifecyclePhase phase) {
            this.beforeGeometryPhases.add(phase);
            return this;
        }

        public Builder addRenderPhase(PipelineRenderPhase phase) {
            this.renderPhases.add(phase);
            return this;
        }

        public Builder addCleanupPhase(PipelineLifecyclePhase phase) {
            this.cleanupPhases.add(phase);
            return this;
        }

        public PipelinePhaseCollection build() {
            return new PipelinePhaseCollection(setupPhases, beforeGeometryPhases, geometryPhases, renderPhases, cleanupPhases);
        }
    }
}
