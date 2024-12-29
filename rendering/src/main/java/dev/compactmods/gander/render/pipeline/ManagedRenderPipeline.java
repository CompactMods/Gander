package dev.compactmods.gander.render.pipeline;

import dev.compactmods.gander.render.pipeline.context.LevelRenderingContext;

public interface ManagedRenderPipeline<TCtx extends LevelRenderingContext> {

    RenderPipelineLifecycleManager<TCtx> createLifecycleManager();
}
