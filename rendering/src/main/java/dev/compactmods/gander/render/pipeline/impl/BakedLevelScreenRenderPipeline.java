package dev.compactmods.gander.render.pipeline.impl;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.RenderPipelineBuilder;
import dev.compactmods.gander.render.pipeline.SinglePassRenderPipeline;
import dev.compactmods.gander.render.screen.GanderScreenPipelinePhases;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import dev.compactmods.gander.render.screen.GanderScreenToolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;

import java.util.OptionalDouble;
import java.util.OptionalInt;

public class BakedLevelScreenRenderPipeline {

    public static SinglePassRenderPipeline INSTANCE;

    static {
        var builder = new RenderPipelineBuilder();
        builder.phases()
            .addSetupPhase(GanderScreenToolkit::setupRenderTarget)
            // .addSetupPhase(GanderScreenToolkit::setupTranslucencyChain)
            .addSetupPhase(BakedLevelScreenRenderPipeline::setup)

            .addPreGeometryPhase(GanderScreenToolkit::switchToFabulous)
            .addGeometryUploadPhase(GanderScreenPipelinePhases::uploadStaticGeometry)
//            .addGeometryUploadPhase(GanderScreenPipelinePhases.BLOCK_ENTITIES_GEOMETRY_UPLOAD)
//            .addGeometryUploadPhase(GanderScreenPipelinePhases.TRANSLUCENT_GEOMETRY_UPLOAD)
            .addRenderPhase(GanderScreenPipelinePhases::renderStaticGeometry)
            .addCleanupPhase(GanderScreenToolkit::revertGraphicsMode)
            .addCleanupPhase(BakedLevelScreenRenderPipeline::teardown);

        INSTANCE = builder.singlePass();
    }

    private static boolean setup(PipelineState state) {
        final var mc = Minecraft.getInstance();
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var renderTarget = state.get(GanderRenderToolkit.RENDER_TARGET);
//        final var translucencyChain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);

        var width = mc.getWindow().getWidth();
        var height = mc.getWindow().getHeight();

        if (width != renderTarget.width || height != renderTarget.height) {
            renderTarget.resize(width, height);

            // TODO 21.5 Port Translucency
//            translucencyChain.resize(renderTarget.width, renderTarget.height);
//            bakedLevel.resortTranslucency(camera.getPosition().toVector3f());
        }

        GanderScreenToolkit.backupProjectionMatrix(state);

        // Setup Render Target
        var mainTarget = mc.getMainRenderTarget();
//        translucencyChain.clear();
//        translucencyChain.prepareBackgroundColor(mainTarget);
//        renderTarget.bindWrite(true);

        return true;
    }



    private static void render(PipelineState state, GuiGraphics graphics) {
        final var mc = Minecraft.getInstance();

        final var projectionMatrix = state.get(GanderRenderToolkit.PROJECTION_MATRIX);
        final var renderTarget = state.get(GanderRenderToolkit.RENDER_TARGET);
        final var renderBounds = state.get(GanderRenderToolkit.RENDER_BOUNDS);
//        final var translucencyChain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);

//        translucencyChain.process();

//        mc.getMainRenderTarget().bindWrite(true);

        RenderSystem.assertOnRenderThread();
//        GlStateManager._colorMask(true, true, true, false);
//        GlStateManager._disableDepthTest();
//        GlStateManager._depthMask(false);

        var guiScale = mc.getWindow().getGuiScale();

        GlStateManager._viewport(
            Mth.floor(renderBounds.left() * guiScale),
            mc.getWindow().getHeight() - Mth.floor(renderBounds.top() * guiScale) - Mth.floor(renderBounds.height() * guiScale),
            Mth.floor((renderBounds.width()) * guiScale),
            Mth.floor((renderBounds.height()) * guiScale)
        );

        Minecraft minecraft = Minecraft.getInstance();
//        ShaderInstance shaderinstance = (ShaderInstance) Objects.requireNonNull(minecraft.gameRenderer.blitShader, "Blit shader not loaded");
//        shaderinstance.setSampler("DiffuseSampler", renderTarget.getColorTextureId());
//        shaderinstance.apply();


        var bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
        bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
        bufferbuilder.addVertex(1.0F, 0.0F, 0.0F);
        bufferbuilder.addVertex(1.0F, 1.0F, 0.0F);
        bufferbuilder.addVertex(0.0F, 1.0F, 0.0F);
        try (final var mesh = bufferbuilder.build()) {

            final var buffer = RenderSystem.getDevice()
                .createBuffer(() -> "Gander Render Buffer", BufferType.VERTICES, BufferUsage.STATIC_WRITE, mesh.vertexBuffer());

            try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(renderTarget.getColorTexture(), OptionalInt.empty(), renderTarget.getDepthTexture(), OptionalDouble.empty())) {

                renderpass.setPipeline(RenderPipelines.GUI);
                renderpass.setVertexBuffer(0, buffer);
                renderpass.draw(0, 10);
            }
        }

//        BufferBuilder bufferbuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);

    }

    private static boolean teardown(PipelineState state) {
        final var renderTarget = state.get(GanderRenderToolkit.RENDER_TARGET);
//        final var translucencyChain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
//
//        renderTarget.clear(Minecraft.ON_OSX);
//        translucencyChain.clear();

        GanderScreenToolkit.restoreProjectionMatrix(state);
        return true;
    }
}
