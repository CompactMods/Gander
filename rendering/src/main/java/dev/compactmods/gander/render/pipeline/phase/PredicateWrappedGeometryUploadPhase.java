package dev.compactmods.gander.render.pipeline.phase;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.PipelineState;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.RenderType;

import org.joml.Matrix4f;

import java.util.function.Predicate;

public record PredicateWrappedGeometryUploadPhase<TCtx>(Predicate<RenderType> predicate, PipelineGeometryUploadPhase<TCtx> phase) implements PipelineGeometryUploadPhase<TCtx> {

    @Override
    public boolean shouldRun(RenderType type) {
        if(predicate == null) return true;
        return predicate.test(type);
    }

    @Override
    public void upload(PipelineState state, TCtx context, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, float partialTicks) {
        phase.upload(state, context, graphics, camera, poseStack, projectionMatrix, modelViewMatrix, partialTicks);
    }
}
