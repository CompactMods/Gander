package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.phase.PipelinePhaseCollection;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

public record SingleEntrypointRenderPipeline<TCtx>(PipelinePhaseCollection<TCtx> phases)
    implements SinglePassRenderPipeline<TCtx> {

    @Override
    public PipelineState setup() {
        PipelineState state = new PipelineState();

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

    public void setupContext(PipelineState state, TCtx context, Camera camera) {
        boolean invalid = false;
        for (var phase : phases.contextSetupPhases()) {
            if (!phase.setup(state, context, camera)) {
                invalid = true;
                break;
            }
        }

        if (invalid)
            throw new RuntimeException("Failed to setup pipeline");
    }

    @Override
    public void render(PipelineState state, TCtx ctx, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix) {
        for (var preRenderPhase : phases.beforeGeometryPhases())
            preRenderPhase.run(state);

        for (var phase : phases.geometryUploadPhases())
            phase.upload(state, ctx, graphics, camera, poseStack, projectionMatrix);

        for (var phase : phases.renderPhases())
            phase.render(state, ctx, graphics, camera, poseStack, projectionMatrix);

        for (var phase : phases.cleanupPhases())
            phase.run(state);
    }
}
