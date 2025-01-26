package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.RenderType;

import net.minecraft.client.renderer.culling.Frustum;

import org.joml.Matrix4f;

public interface MultiPassRenderPipeline extends RenderPipeline {

    void renderPass(PipelineState state, RenderType renderType, GuiGraphics graphics, Frustum frustum, float partialTicks);

}
