package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.phase.ContextAwareSetupPhase;
import dev.compactmods.gander.render.pipeline.phase.PipelineGeometryUploadPhase;
import dev.compactmods.gander.render.pipeline.phase.PipelineLifecyclePhase;
import dev.compactmods.gander.render.pipeline.phase.PipelineRenderPhase;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Matrix4f;

import java.util.Set;

public record SingleEntrypointRenderPipeline<TCtx>(Set<PipelineLifecyclePhase> setupPhases,
                                                   Set<ContextAwareSetupPhase<TCtx>> contextSetupPhases,
                                                   Set<PipelineLifecyclePhase> cleanupPhases,
                                                   Set<PipelineGeometryUploadPhase<TCtx>> geometryPhases,
                                                   Set<PipelineRenderPhase<TCtx>> renderPhases)
    implements SinglePassRenderPipeline<TCtx> {

    @Override
    public PipelineState setup() {
        PipelineState state = new PipelineState();

        boolean invalid = false;
        for(var phase : setupPhases) {
            if (!phase.run(state)) {
                invalid = true;
                break;
            }
        }

        if(invalid)
            throw new RuntimeException("Failed to setup pipeline");

        return state;
    }

    public void setupContext(PipelineState state, TCtx context, Camera camera) {
        boolean invalid = false;
        for(var phase : contextSetupPhases) {
            if(!phase.setup(state, context, camera)) {
                invalid = true;
                break;
            }
        }

        if(invalid)
            throw new RuntimeException("Failed to setup pipeline");
    }

    @Override
    public void render(PipelineState state, TCtx ctx, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix) {
        for(var phase : geometryPhases)
            phase.upload(state, ctx, graphics, camera, poseStack, projectionMatrix);

        for(var phase : renderPhases)
            phase.render(state, ctx, graphics, camera, poseStack, projectionMatrix);

        for(var phase : cleanupPhases)
            phase.run(state);
    }
}
