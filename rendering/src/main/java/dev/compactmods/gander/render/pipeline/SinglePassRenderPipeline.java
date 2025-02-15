package dev.compactmods.gander.render.pipeline;

import net.minecraft.client.gui.GuiGraphics;

public interface SinglePassRenderPipeline extends RenderPipeline {

    void render(PipelineState state, GuiGraphics graphics);

}
