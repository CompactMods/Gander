package dev.compactmods.gander.render.pipeline;

import com.mojang.blaze3d.systems.CommandEncoder;

import dev.compactmods.gander.render.pipeline.phase.PipelinePhaseCollection;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;

import java.util.function.Consumer;

public record StagedMultipassRenderPipeline(PipelinePhaseCollection phaseCollection) implements MultiPassRenderPipeline {

    @Override
    public void renderPass(PipelineState state, CommandEncoder encoder, float partialTicks) {
        for (var preRenderPhase : phaseCollection.beforeGeometryPhases())
            preRenderPhase.run(state);

        for (var phase : phaseCollection.geometryUploadPhases()) {
            phase.upload(state, encoder);
        }

        for (var phase : phaseCollection.renderPhases())
            phase.render(state, encoder, partialTicks);

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
