package dev.compactmods.gander.render.pipeline;

import dev.compactmods.gander.render.pipeline.context.LevelRenderingContext;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface PipelineLifecyclePhase<TCtx extends LevelRenderingContext> {
    boolean run(PipelineState state, TCtx ctx, GuiGraphics graphics, Camera camera);
}
