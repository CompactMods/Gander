package dev.compactmods.gander.render.pipeline.phase;

import com.mojang.blaze3d.systems.CommandEncoder;

import dev.compactmods.gander.render.pipeline.PipelineState;
import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface PipelineRenderPhase extends PipelinePhase {

    void render(PipelineState state, CommandEncoder encoder, float partialTicks);

}
