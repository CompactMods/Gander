package dev.compactmods.gander.ui.pipeline;

import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.RenderPipelineBuilder;
import dev.compactmods.gander.render.pipeline.SinglePassRenderPipeline;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenRenderingContext;
import dev.compactmods.gander.ui.toolkit.GanderScreenToolkit;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

public class BakedLevelScreenRenderPipeline {

    public static SinglePassRenderPipeline<BakedLevelScreenRenderingContext> INSTANCE = new RenderPipelineBuilder<BakedLevelScreenRenderingContext>()
        .addSetupPhase(GanderScreenToolkit::switchToFabulous)
        .addSetupPhase(GanderScreenToolkit::setupBasicRenderRequirements)
        .addSetupPhase(BakedLevelScreenRenderPipeline::setup)
        .addGeometryUploadPhase(GanderScreenPipelinePhases.STATIC_GEOMETRY_UPLOAD)
        .addGeometryUploadPhase(GanderScreenPipelinePhases.BLOCK_ENTITIES_GEOMETRY_UPLOAD)
        .addGeometryUploadPhase(GanderScreenPipelinePhases.TRANSLUCENT_GEOMETRY_UPLOAD)
        .addCleanupPhase(BakedLevelScreenRenderPipeline::teardown)
        .addCleanupPhase(GanderScreenToolkit::revertGraphicsMode)
        .singlePass();

    private static boolean setup(PipelineState state, BakedLevelScreenRenderingContext ctx, Camera camera) {
        final var mc = Minecraft.getInstance();
        final var renderTarget = state.get(GanderScreenToolkit.RENDER_TARGET);
        final var translucencyChain = state.get(GanderScreenToolkit.TRANSLUCENCY_CHAIN);

        var width = mc.getWindow().getWidth();
        var height = mc.getWindow().getHeight();

        if (width != renderTarget.width || height != renderTarget.height) {
            renderTarget.resize(width, height, Minecraft.ON_OSX);
            translucencyChain.resize(renderTarget.width, renderTarget.height);

            ctx.recalculateTranslucency(camera);
        }

        GanderScreenToolkit.backupProjectionMatrix(state);

        // Setup Render Target
        var mainTarget = mc.getMainRenderTarget();
        translucencyChain.clear();
        translucencyChain.prepareBackgroundColor(mainTarget);
        renderTarget.bindWrite(true);

        return true;
    }

    private static boolean teardown(PipelineState state, BakedLevelScreenRenderingContext ctx, Camera camera) {
        Minecraft mc = Minecraft.getInstance();

        final var renderTarget = state.get(GanderScreenToolkit.RENDER_TARGET);
        final var translucencyChain = state.get(GanderScreenToolkit.TRANSLUCENCY_CHAIN);

        translucencyChain.process();

        mc.getMainRenderTarget().bindWrite(true);
        renderTarget.blitToScreen(renderTarget.width, renderTarget.height, false);

        renderTarget.clear(Minecraft.ON_OSX);
        translucencyChain.clear();

        GanderScreenToolkit.restoreProjectionMatrix(state);
        return true;
    }
}
