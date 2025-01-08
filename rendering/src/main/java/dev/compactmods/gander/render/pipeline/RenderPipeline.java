package dev.compactmods.gander.render.pipeline;

import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A render pipeline is a pre-configured set of rendering steps for rendering a
 * level to a specific target.
 */
public interface RenderPipeline<TCtx> {

    PipelineState setup(TCtx ctx, Camera camera);

}
