package dev.compactmods.gander.ui.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.RenderPipeline;
import dev.compactmods.gander.render.pipeline.RenderPipelineLifecycleManager;
import dev.compactmods.gander.render.pipeline.ManagedRenderPipeline;
import dev.compactmods.gander.render.toolkit.BlockEntityRender;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenLevelRenderingContext;
import dev.compactmods.gander.ui.pipeline.init.BakedVirtualLevelScreenPipelineLifecycleManager;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;

import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.Objects;

public final class BakedVirtualLevelScreenPipeline implements
    RenderPipeline<BakedLevelScreenLevelRenderingContext>,
    ManagedRenderPipeline<BakedLevelScreenLevelRenderingContext> {

    @Override
    public RenderPipelineLifecycleManager<BakedLevelScreenLevelRenderingContext> createLifecycleManager() {
        return new BakedVirtualLevelScreenPipelineLifecycleManager();
    }

    @Override
    public void staticGeometryPass(BakedLevelScreenLevelRenderingContext ctx, GuiGraphics graphics, float partialTick, RenderType renderType, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc renderOffset) {
        BlockRenderer.renderSectionBlocks(ctx.bakedLevel, ctx.renderTypeStore, renderType, poseStack, camera.getLookVector(), projectionMatrix);
        BlockRenderer.renderSectionFluids(ctx.bakedLevel, ctx.renderTypeStore, renderType, poseStack, camera.getLookVector(), projectionMatrix);
    }

    @Override
    public void blockEntitiesPass(BakedLevelScreenLevelRenderingContext ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource, Vector3fc renderOffset) {
        final var blockEntities = ctx.blockEntityPositions
            .stream()
            .map(ctx.blockAndTints::getBlockEntity)
            .filter(Objects::nonNull);

        BlockEntityRender.render(ctx.blockAndTints, blockEntities, poseStack, camera.getLookVector(), ctx.renderTypeStore, bufferSource, partialTick);
    }
}
