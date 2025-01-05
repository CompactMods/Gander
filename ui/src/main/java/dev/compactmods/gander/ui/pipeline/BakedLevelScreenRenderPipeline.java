package dev.compactmods.gander.ui.pipeline;

import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.RenderPipeline;
import dev.compactmods.gander.render.pipeline.RenderPipelineBuilder;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenRenderingContext;
import dev.compactmods.gander.ui.toolkit.GanderScreenToolkit;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class BakedLevelScreenRenderPipeline {

    public static RenderPipeline<BakedLevelScreenRenderingContext> INSTANCE = new RenderPipelineBuilder<BakedLevelScreenRenderingContext>()
        .addSetupPhase(GanderScreenToolkit::setupRenderRequirements)
        .addSetupPhase(GanderScreenToolkit::switchToFabulous)
        .addSetupPhase(BakedLevelScreenRenderPipeline::setup)
        .addGeometryUploadPhase(GanderScreenPipelinePhases.STATIC_GEOMETRY_UPLOAD)
        .addGeometryUploadPhase(GanderScreenPipelinePhases.BLOCK_ENTITIES_GEOMETRY_UPLOAD)
        .addGeometryUploadPhase(GanderScreenPipelinePhases.TRANSLUCENT_GEOMETRY_UPLOAD)
        .addCleanupPhase(BakedLevelScreenRenderPipeline::teardown)
        .addCleanupPhase(GanderScreenToolkit::revertGraphicsMode)
        .build();

    private static boolean setup(PipelineState state, BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, Camera camera) {
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

    private static boolean teardown(PipelineState state, BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, Camera camera) {
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
