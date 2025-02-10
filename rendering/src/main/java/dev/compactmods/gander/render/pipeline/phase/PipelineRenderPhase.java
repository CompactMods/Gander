package dev.compactmods.gander.render.pipeline.phase;

import dev.compactmods.gander.render.pipeline.PipelineState;
import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface PipelineRenderPhase extends PipelinePhase {

    void render(PipelineState state, GuiGraphics graphics);

}
