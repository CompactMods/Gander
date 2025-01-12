package dev.compactmods.gander.render.pipeline.phase;

import dev.compactmods.gander.render.pipeline.PipelineState;

@FunctionalInterface
public interface PipelineLifecyclePhase {
    boolean run(PipelineState state);
}
