package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.phase.PipelinePhaseCollection;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;

import org.joml.Matrix4f;

import java.util.function.Consumer;

public record StagedMultipassRenderPipeline(PipelinePhaseCollection phaseCollection) implements MultiPassRenderPipeline {

    @Override
    public void renderPass(PipelineState state, RenderType renderType, GuiGraphics graphics, Camera camera, Frustum frustum,
                           PoseStack poseStack, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, float partialTicks) {
        for (var preRenderPhase : phaseCollection.beforeGeometryPhases())
            preRenderPhase.run(state);

        for (var phase : phaseCollection.geometryUploadPhases()) {
            if(phase.shouldRun(renderType))
                phase.upload(state, graphics, camera, projectionMatrix, modelViewMatrix, partialTicks);
        }

        for (var phase : phaseCollection.renderPhases())
            phase.render(state, graphics, camera, projectionMatrix);

        for (var phase : phaseCollection.cleanupPhases())
            phase.run(state);
    }

    @Override
    public PipelineState setup(Consumer<PipelineState> initializer) {
        final var state = new PipelineState();
        initializer.accept(state);
        return state;
    }
}
