package dev.compactmods.gander.render.pipeline;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public interface StageAwareRenderPipeline {

    void handleStageEvent(RenderLevelStageEvent event, PipelineState state);
}
