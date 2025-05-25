package dev.compactmods.gander.render.pipeline.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import com.mojang.blaze3d.vertex.VertexFormat;

import dev.compactmods.gander.render.event.GanderRenderLevelStageEvent;
import dev.compactmods.gander.render.pipeline.PipelineHelper;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.SinglePassRenderPipeline;
import dev.compactmods.gander.render.pipeline.phase.PipelinePhaseCollection;
import dev.compactmods.gander.render.screen.GanderScreenPipelinePhases;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import dev.compactmods.gander.render.screen.GanderScreenToolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.ShaderInstance;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record BakedLevelScreenRenderPipeline(PipelinePhaseCollection phases) implements SinglePassRenderPipeline {

    public static SinglePassRenderPipeline INSTANCE;

    static {
        final var phases = new PipelinePhaseCollection.Builder()
            .addSetupPhase(GanderRenderToolkit::makeDeltaTracker)
            .addSetupPhase(GanderScreenToolkit::setupRenderTarget)
            .addSetupPhase(GanderScreenToolkit::setupTranslucencyChain)
            .addSetupPhase(BakedLevelScreenRenderPipeline::setup)
            .addLazySetupPhase(BakedLevelScreenRenderPipeline::setupLevelRenderer)

            .addPreGeometryPhase(GanderScreenToolkit::switchToFabulous)
            .addPreGeometryPhase(BakedLevelScreenRenderPipeline::setupCullFrustum)
            .addGeometryUploadPhase(GanderScreenPipelinePhases.STATIC_GEOMETRY_UPLOAD)
            .addGeometryUploadPhase(BakedLevelScreenRenderPipeline::fireStaticLevelStageEvent)
            .addGeometryUploadPhase(GanderScreenPipelinePhases.BLOCK_ENTITIES_GEOMETRY_UPLOAD)
            .addGeometryUploadPhase(GanderScreenPipelinePhases.TRANSLUCENT_GEOMETRY_UPLOAD)
            .addGeometryUploadPhase(BakedLevelScreenRenderPipeline::fireAfterTranslucentLevelStageEvent)
            .addGeometryUploadPhase(BakedLevelScreenRenderPipeline::fireAfterParticlesLevelStageEvent)
            .addRenderPhase(BakedLevelScreenRenderPipeline::uploadBuffer)
            .addCleanupPhase(GanderScreenToolkit::revertGraphicsMode)
            .addCleanupPhase(BakedLevelScreenRenderPipeline::teardown)
            .build();

        INSTANCE = new BakedLevelScreenRenderPipeline(phases);
    }

    private static boolean setupCullFrustum(PipelineState state) {
        final var frustum = GanderScreenToolkit.makeCullFrustum(state);
        state.set(GanderRenderToolkit.CULLING_FRUSTUM, frustum);
        return true;
    }

    private static boolean setupLevelRenderer(Supplier<PipelineState> pipelineStateSupplier) {
        final var state = pipelineStateSupplier.get();
        final var levelRenderer = GanderScreenToolkit.makeLevelRenderer(pipelineStateSupplier);
        state.set(GanderRenderToolkit.LEVEL_RENDERER, levelRenderer);
        return true;
    }

    private static void fireStaticLevelStageEvent(PipelineState state, GuiGraphics graphics) {
        final var event = GanderRenderLevelStageEvent.make(RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS, state);

        NeoForge.EVENT_BUS.post(event);
    }

    private static void fireAfterParticlesLevelStageEvent(PipelineState state, GuiGraphics graphics) {
        final var event = GanderRenderLevelStageEvent.make(RenderLevelStageEvent.Stage.AFTER_PARTICLES, state);

        NeoForge.EVENT_BUS.post(event);
    }

    private static void fireAfterTranslucentLevelStageEvent(PipelineState state, GuiGraphics graphics) {
//        try {
            final var event = GanderRenderLevelStageEvent.make(RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS, state);

            NeoForge.EVENT_BUS.post(event);
//        }
//
//        catch(Exception e) {
//            //
//        }
    }

    private static boolean setup(PipelineState state) {
        final var mc = Minecraft.getInstance();
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var renderTarget = state.get(GanderRenderToolkit.RENDER_TARGET);
        final var translucencyChain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);

        var width = mc.getWindow().getWidth();
        var height = mc.getWindow().getHeight();

        if (width != renderTarget.width || height != renderTarget.height) {
            renderTarget.resize(width, height, Minecraft.ON_OSX);
            translucencyChain.resize(renderTarget.width, renderTarget.height);

            bakedLevel.resortTranslucency(camera.getPosition().toVector3f());
        }

        GanderScreenToolkit.backupProjectionMatrix(state);

        state.set(GanderRenderToolkit.MODEL_VIEW_MATRIX, GanderScreenToolkit.getViewMatrix(camera));

        // Setup Render Target
        var mainTarget = mc.getMainRenderTarget();
        translucencyChain.clear();
        translucencyChain.prepareBackgroundColor(mainTarget);
        renderTarget.bindWrite(true);

        return true;
    }

    private static void uploadBuffer(PipelineState state, GuiGraphics graphics) {
        final var mc = Minecraft.getInstance();

        final var projectionMatrix = state.get(GanderRenderToolkit.PROJECTION_MATRIX);
        final var renderTarget = state.get(GanderRenderToolkit.RENDER_TARGET);
        final var renderBounds = state.get(GanderRenderToolkit.RENDER_BOUNDS);
        final var translucencyChain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);

        translucencyChain.process();

        mc.getMainRenderTarget().bindWrite(true);

        RenderSystem.assertOnRenderThread();
        GlStateManager._colorMask(true, true, true, false);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);

        var guiScale = mc.getWindow().getGuiScale();

        GlStateManager._viewport(
            Mth.floor(renderBounds.left() * guiScale),
            mc.getWindow().getHeight() - Mth.floor(renderBounds.top() * guiScale) - Mth.floor(renderBounds.height() * guiScale),
            Mth.floor((renderBounds.width()) * guiScale),
            Mth.floor((renderBounds.height()) * guiScale)
        );

        Minecraft minecraft = Minecraft.getInstance();
        ShaderInstance shaderinstance = Objects.requireNonNull(minecraft.gameRenderer.blitShader, "Blit shader not loaded");
        shaderinstance.setSampler("DiffuseSampler", renderTarget.getColorTextureId());
        shaderinstance.apply();
        BufferBuilder bufferbuilder = RenderSystem.renderThreadTesselator()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);

        bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
        bufferbuilder.addVertex(1.0F, 0.0F, 0.0F);
        bufferbuilder.addVertex(1.0F, 1.0F, 0.0F);
        bufferbuilder.addVertex(0.0F, 1.0F, 0.0F);
        BufferUploader.draw(bufferbuilder.buildOrThrow());
        shaderinstance.clear();

        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
    }

    private static boolean teardown(PipelineState state) {
        final var renderTarget = state.get(GanderRenderToolkit.RENDER_TARGET);
        final var translucencyChain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);

        renderTarget.clear(Minecraft.ON_OSX);
        translucencyChain.clear();

        GanderScreenToolkit.restoreProjectionMatrix(state);
        return true;
    }

    @Override
    public PipelineState setup(Consumer<PipelineState> initialStateSetup) {
        PipelineState state = new PipelineState();
        initialStateSetup.accept(state);

        if (!PipelineHelper.runStandardPipelineSetup(phases, state))
            throw new RuntimeException("Failed to setup pipeline");

        return state;
    }

    @Override
    public void render(PipelineState state, GuiGraphics graphics) {
        for (var preRenderPhase : phases.beforeGeometryPhases())
            preRenderPhase.run(state);

        for (var phase : phases.geometryUploadPhases())
            phase.upload(state, graphics);

        for (var phase : phases.renderPhases())
            phase.render(state, graphics);

        for (var phase : phases.cleanupPhases())
            phase.run(state);
    }
}
