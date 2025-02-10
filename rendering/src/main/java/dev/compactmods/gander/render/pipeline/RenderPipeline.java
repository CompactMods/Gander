package dev.compactmods.gander.render.pipeline;

import java.util.function.Consumer;

/**
 * A render pipeline is a pre-configured set of rendering steps for rendering a
 * level to a specific target.
 */
public interface RenderPipeline {

    PipelineState setup(Consumer<PipelineState> initialStateSetup);

}
