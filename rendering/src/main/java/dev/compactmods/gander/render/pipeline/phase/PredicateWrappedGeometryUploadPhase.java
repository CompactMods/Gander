package dev.compactmods.gander.render.pipeline.phase;

import dev.compactmods.gander.render.pipeline.PipelineState;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.RenderType;

import org.joml.Matrix4f;

import java.util.function.Predicate;

public record PredicateWrappedGeometryUploadPhase(Predicate<RenderType> predicate, PipelineGeometryUploadPhase phase) implements PipelineGeometryUploadPhase {

    @Override
    public boolean shouldRun(RenderType type) {
        if(predicate == null) return true;
        return predicate.test(type);
    }

    @Override
    public void upload(PipelineState state, GuiGraphics graphics, Camera camera, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, float partialTicks) {
        phase.upload(state, graphics, camera, projectionMatrix, modelViewMatrix, partialTicks);
    }
}
