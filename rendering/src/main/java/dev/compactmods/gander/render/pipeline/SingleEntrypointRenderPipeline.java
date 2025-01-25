package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.phase.PipelinePhaseCollection;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

import java.util.function.Consumer;

public record SingleEntrypointRenderPipeline(PipelinePhaseCollection phases)
    implements SinglePassRenderPipeline {

    @Override
    public PipelineState setup(Consumer<PipelineState> stateInitializer) {
        PipelineState state = new PipelineState();
        stateInitializer.accept(state);

        boolean invalid = false;
        for (var phase : phases.setupPhases()) {
            if (!phase.run(state)) {
                invalid = true;
                break;
            }
        }

        if (invalid)
            throw new RuntimeException("Failed to setup pipeline");

        return state;
    }

    @Override
    public void render(PipelineState state, GuiGraphics graphics, Camera camera, Matrix4f projectionMatrix, Matrix4f viewMatrix, float partialTicks) {
        for (var preRenderPhase : phases.beforeGeometryPhases())
            preRenderPhase.run(state);

        for (var phase : phases.geometryUploadPhases())
            phase.upload(state, graphics, camera, projectionMatrix, viewMatrix, partialTicks);

        for (var phase : phases.renderPhases())
            phase.render(state, graphics, camera, projectionMatrix);

        for (var phase : phases.cleanupPhases())
            phase.run(state);
    }
}
