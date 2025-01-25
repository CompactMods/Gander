package dev.compactmods.gander.render.pipeline.phase;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.PipelineState;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.RenderType;

import org.joml.Matrix4f;

public interface PipelineGeometryUploadPhase extends PipelinePhase {

    default boolean shouldRun(RenderType renderType) {
        return true;
    }

    void upload(PipelineState state, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, float partialTicks);
}
