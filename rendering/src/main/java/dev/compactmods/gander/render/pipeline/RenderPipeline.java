package dev.compactmods.gander.render.pipeline;

import net.minecraft.client.Camera;

/**
 * A render pipeline is a pre-configured set of rendering steps for rendering a
 * level to a specific target.
 */
public interface RenderPipeline<TCtx> {

    PipelineState setup();

    default void setupContext(PipelineState state, TCtx ctx, Camera camera) {

    }

}
