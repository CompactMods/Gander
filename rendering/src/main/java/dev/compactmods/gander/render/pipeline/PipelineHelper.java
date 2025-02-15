package dev.compactmods.gander.render.pipeline;

import dev.compactmods.gander.render.pipeline.phase.PipelinePhaseCollection;

import java.util.function.Supplier;

public class PipelineHelper {

    /**
     * Sets up a standard pipeline that has one or more setup phases.
     *
     * @param phases
     * @param state
     * @return True if all setup phases completed; false otherwise.
     */
    public static boolean runStandardPipelineSetup(PipelinePhaseCollection phases, PipelineState state) {
        for (var phase : phases.setupPhases()) {
            if (!phase.run(state)) {
                return false;
            }
        }

        Supplier<PipelineState> stateSupplier = () -> state;
        for (var phase : phases.lazySetupPhases()) {
            if (!phase.run(stateSupplier)) {
                return false;
            }
        }

        return true;
    }

}
