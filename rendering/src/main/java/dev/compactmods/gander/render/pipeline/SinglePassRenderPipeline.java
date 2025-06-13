package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.systems.CommandEncoder;

public interface SinglePassRenderPipeline extends RenderPipeline {

    void render(PipelineState state, CommandEncoder encoder, float partialTicks);

}
