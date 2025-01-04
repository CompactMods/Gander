package dev.compactmods.gander.ui.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.RenderPipeline;
import dev.compactmods.gander.render.pipeline.RenderPipelineLifecycleManager;
import dev.compactmods.gander.render.pipeline.ManagedRenderPipeline;
import dev.compactmods.gander.render.toolkit.BlockEntityRender;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenRenderingContext;
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
    RenderPipeline<BakedLevelScreenRenderingContext>,
    ManagedRenderPipeline<BakedLevelScreenRenderingContext> {

    @Override
    public RenderPipelineLifecycleManager<BakedLevelScreenRenderingContext> createLifecycleManager() {
        return new BakedVirtualLevelScreenPipelineLifecycleManager();
    }

    @Override
    public void staticGeometryPass(BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, float partialTick, RenderType renderType, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc renderOffset) {
        final var lookFrom = camera.getPosition().toVector3f();

        BlockRenderer.renderSectionBlocks(ctx.bakedLevel, ctx.renderTypeStore, renderType, poseStack, lookFrom, projectionMatrix);
        BlockRenderer.renderSectionFluids(ctx.bakedLevel, ctx.renderTypeStore, renderType, poseStack, lookFrom, projectionMatrix);
    }

    @Override
    public void blockEntitiesPass(BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource, Vector3fc renderOffset) {
        final var blockEntities = ctx.blockEntityPositions
            .stream()
            .map(ctx.blockAndTints::getBlockEntity)
            .filter(Objects::nonNull);

        final var lookFrom = camera.getPosition().toVector3f();
        BlockEntityRender.render(ctx.blockAndTints, blockEntities, poseStack, lookFrom, ctx.renderTypeStore, bufferSource, partialTick);
    }
}
