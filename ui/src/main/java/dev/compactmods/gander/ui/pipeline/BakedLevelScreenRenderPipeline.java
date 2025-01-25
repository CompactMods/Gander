package dev.compactmods.gander.ui.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexFormat;

import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.RenderPipelineBuilder;
import dev.compactmods.gander.render.pipeline.SinglePassRenderPipeline;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import dev.compactmods.gander.ui.toolkit.GanderScreenToolkit;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.ShaderInstance;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.GlStateBackup;

import org.joml.Matrix4f;

import java.util.Objects;

public class BakedLevelScreenRenderPipeline {

    public static SinglePassRenderPipeline INSTANCE;

    static {
        var builder = new RenderPipelineBuilder();
        builder.phases()
            .addSetupPhase(GanderScreenToolkit::setupRenderTarget)
            .addSetupPhase(GanderScreenToolkit::setupTranslucencyChain)
            .addSetupPhase(BakedLevelScreenRenderPipeline::setup)

            .addPreGeometryPhase(GanderScreenToolkit::switchToFabulous)
            .addGeometryUploadPhase(GanderScreenPipelinePhases.STATIC_GEOMETRY_UPLOAD)
            .addGeometryUploadPhase(GanderScreenPipelinePhases.BLOCK_ENTITIES_GEOMETRY_UPLOAD)
            .addGeometryUploadPhase(GanderScreenPipelinePhases.TRANSLUCENT_GEOMETRY_UPLOAD)
            .addRenderPhase(BakedLevelScreenRenderPipeline::render)
            .addCleanupPhase(GanderScreenToolkit::revertGraphicsMode)
            .addCleanupPhase(BakedLevelScreenRenderPipeline::teardown);

        INSTANCE = builder.singlePass();
    }

    private static boolean setup(PipelineState state) {
        final var mc = Minecraft.getInstance();
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);
//        final var renderBounds = state.get(GanderRenderToolkit.RENDER_BOUNDS);
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

        // Setup Render Target
        var mainTarget = mc.getMainRenderTarget();
        translucencyChain.clear();
        translucencyChain.prepareBackgroundColor(mainTarget);
        renderTarget.bindWrite(true);

        return true;
    }

    private static void render(PipelineState state, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix) {
        final var mc = Minecraft.getInstance();
        final var renderTarget = state.get(GanderRenderToolkit.RENDER_TARGET);
        final var renderBounds = state.get(GanderRenderToolkit.RENDER_BOUNDS);
        final var translucencyChain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);

        translucencyChain.process();

        mc.getMainRenderTarget().bindWrite(true);

        renderTarget.blitToScreen(renderTarget.width, renderTarget.height, false);
    }

    private static boolean teardown(PipelineState state) {
        final var renderTarget = state.get(GanderRenderToolkit.RENDER_TARGET);
        final var translucencyChain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);

        renderTarget.clear(Minecraft.ON_OSX);
        translucencyChain.clear();

        GanderScreenToolkit.restoreProjectionMatrix(state);
        return true;
    }
}
