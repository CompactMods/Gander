package dev.compactmods.gander.render.pipeline;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.RenderType;

import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.culling.Frustum;

public interface MultiPassRenderPipeline extends RenderPipeline {

    void renderPass(PipelineState state, ChunkSectionLayerGroup layerGroup, Frustum frustum);

}
