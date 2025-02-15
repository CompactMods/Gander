package dev.compactmods.gander.render.pipeline;

import dev.compactmods.gander.render.pipeline.phase.PipelinePhaseCollection;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;

public record SingleEntrypointRenderPipeline(PipelinePhaseCollection phases)
    implements SinglePassRenderPipeline {

    @Override
    public PipelineState setup(Consumer<PipelineState> stateInitializer) {
        PipelineState state = new PipelineState();
        stateInitializer.accept(state);

        if (!PipelineHelper.runStandardPipelineSetup(phases, state))
            throw new RuntimeException("Failed to setup pipeline");

        return state;
    }

    @Override
    public void render(PipelineState state, GuiGraphics graphics) {
        for (var preRenderPhase : phases.beforeGeometryPhases())
            preRenderPhase.run(state);

        for (var phase : phases.geometryUploadPhases())
            phase.upload(state, graphics);

        for (var phase : phases.renderPhases())
            phase.render(state, graphics);

        for (var phase : phases.cleanupPhases())
            phase.run(state);
    }
}
