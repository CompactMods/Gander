package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.systems.CommandEncoder;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.RenderType;

import net.minecraft.client.renderer.culling.Frustum;

public interface MultiPassRenderPipeline extends RenderPipeline {

    void renderPass(PipelineState state, CommandEncoder encoder, float partialTicks);

}
