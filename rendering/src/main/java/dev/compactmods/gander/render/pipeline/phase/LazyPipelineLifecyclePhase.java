package dev.compactmods.gander.render.pipeline.phase;

import dev.compactmods.gander.render.pipeline.PipelineState;

import java.util.function.Supplier;

public interface LazyPipelineLifecyclePhase extends PipelinePhase {

    boolean run(Supplier<PipelineState> stateSupplier);
}
