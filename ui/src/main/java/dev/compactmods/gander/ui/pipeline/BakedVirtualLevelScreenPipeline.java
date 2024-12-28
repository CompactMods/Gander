package dev.compactmods.gander.ui.pipeline;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.render.RenderPipeline;
import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenLevelRenderingContext;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;

import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.Objects;

public final class BakedVirtualLevelScreenPipeline implements RenderPipeline<BakedLevelScreenLevelRenderingContext> {

    @Override
    public void setup(BakedLevelScreenLevelRenderingContext ctx, GuiGraphics graphics, PoseStack origPoseStack, Camera camera, Matrix4f origProjectionMatrix) {
        final var opts = Minecraft.getInstance().options;
        GraphicsStatus prevGraphicsMode = opts.graphicsMode().get();
        ctx.setPreviousGraphicsStatus(prevGraphicsMode);
        opts.graphicsMode().set(GraphicsStatus.FABULOUS);

        //
        if (!ctx.canRender())
            return;

        final var mc = Minecraft.getInstance();
        final var buffer = graphics.bufferSource();

        setupRenderSizeAndPrepTranslucency(ctx, mc);

        ctx.storePreviousRenderState();

        var projectionMatrix = new Matrix4f().setPerspective(
            (float) Math.PI / 2f,
            (float) ctx.renderTarget.width / (float) ctx.renderTarget.height,
            0.05f,
            10000000);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        var poseStack2 = RenderSystem.getModelViewStack();
        poseStack2.pushMatrix();

        poseStack2.identity();
        RenderSystem.applyModelViewMatrix();

        poseStack.setIdentity();
        poseStack.mulPose(camera.rotation());

        var mainTarget = mc.getMainRenderTarget();
        ctx.translucencyChain.clear();
        ctx.translucencyChain.prepareBackgroundColor(mainTarget);
        ctx.renderTarget.bindWrite(true);

        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.byDistance(ctx.camera.getLookFrom()));
    }

    private static void setupRenderSizeAndPrepTranslucency(BakedLevelScreenLevelRenderingContext ctx, Minecraft mc) {
        var width = mc.getWindow().getWidth();
        var height = mc.getWindow().getHeight();
        if (width != ctx.renderTarget.width || height != ctx.renderTarget.height) {
            ctx.renderTarget.resize(width, height, Minecraft.ON_OSX);
            ctx.translucencyChain.resize(ctx.renderTarget.width, ctx.renderTarget.height);
            ctx.recalculateTranslucency();
        }
    }

    @Override
    public void teardown(BakedLevelScreenLevelRenderingContext ctx, GuiGraphics graphics, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix) {
        Minecraft mc = Minecraft.getInstance();

        mc.getMainRenderTarget().bindWrite(true);
        copyRenderToWidgetSpace(ctx, ctx.renderTarget);

        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.byDistance(ctx.camera.getLookFrom()));

        var poseStack2 = RenderSystem.getModelViewStack();
        poseStack2.popMatrix();
        RenderSystem.applyModelViewMatrix();

        poseStack.popPose();

        ctx.restorePreviousRenderingState();

        // Restore Graphics Mode
        final var opts = Minecraft.getInstance().options;
        opts.graphicsMode().set(ctx.previousGraphicsStatus());
    }

    private void copyRenderToWidgetSpace(BakedLevelScreenLevelRenderingContext ctx, RenderTarget renderTarget) {
        // RenderTarget.blit disables alpha... :unamused:
        RenderSystem.assertOnRenderThread();
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, renderTarget.width, renderTarget.height);

        Minecraft minecraft = Minecraft.getInstance();
        ShaderInstance shaderinstance = minecraft.gameRenderer.blitShader;
        shaderinstance.setSampler("DiffuseSampler", renderTarget.getColorTextureId());
        Matrix4f matrix4f = new Matrix4f().setOrtho(0.0F, (float) ctx.width, (float) ctx.height, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
        if (shaderinstance.MODEL_VIEW_MATRIX != null) {
            shaderinstance.MODEL_VIEW_MATRIX.set(new Matrix4f().translation(0.0F, 0.0F, -2000.0F));
        }

        if (shaderinstance.PROJECTION_MATRIX != null) {
            shaderinstance.PROJECTION_MATRIX.set(matrix4f);
        }

        shaderinstance.apply();
        float f = (float) ctx.width;
        float f1 = (float) ctx.height;
        float f2 = (float) renderTarget.viewWidth / (float) renderTarget.width;
        float f3 = (float) renderTarget.viewHeight / (float) renderTarget.height;

        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferbuilder.addVertex(0f, f1, 0f)
            .setUv(0.0F, 0.0F)
            .setColor(255, 255, 255, 255);

        bufferbuilder.addVertex(f, f1, 0f)
            .setUv(f2, 0.0F)
            .setColor(255, 255, 255, 255);

        bufferbuilder.addVertex(f, 0f, 0f)
            .setUv(f2, f3)
            .setColor(255, 255, 255, 255);

        bufferbuilder.addVertex(0f, 0f, 0f)
            .setUv(0.0F, f3)
            .setColor(255, 255, 255, 255);

        BufferUploader.draw(bufferbuilder.buildOrThrow());
        shaderinstance.clear();
        GlStateManager._depthMask(true);
    }

    @Override
    public void staticGeometryPass(BakedLevelScreenLevelRenderingContext ctx, GuiGraphics graphics, float partialTick, RenderType renderType, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc renderOffset) {
        BlockRenderer.renderSectionBlocks(ctx.bakedLevel, ctx.renderTypeStore, renderType, poseStack, camera.getLookVector(), projectionMatrix);
    }

    @Override
    public void blockEntitiesPass(BakedLevelScreenLevelRenderingContext ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource, Vector3fc renderOffset) {
        final var blockEntities = ctx.blockEntityPositions
            .stream()
            .map(ctx.blockAndTints::getBlockEntity)
            .filter(Objects::nonNull);
    }
}
