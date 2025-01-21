package dev.compactmods.gander.render.pipeline.phase;

import dev.compactmods.gander.render.pipeline.PipelineState;

@FunctionalInterface
public interface PipelineLifecyclePhase extends PipelinePhase {
    boolean run(PipelineState state);
}
