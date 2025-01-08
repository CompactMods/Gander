package dev.compactmods.gander.render.pipeline;

import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface PipelineLifecyclePhase<TCtx> {
    boolean run(PipelineState state, TCtx ctx, Camera camera);
}
