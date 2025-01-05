package dev.compactmods.gander.ui.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.render.pipeline.RenderPipeline;
import dev.compactmods.gander.render.pipeline.RenderPipelineLifecycleManager;
import dev.compactmods.gander.render.pipeline.ManagedRenderPipeline;
import dev.compactmods.gander.render.toolkit.BlockEntityRender;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenRenderingContext;
import dev.compactmods.gander.ui.pipeline.init.BakedVirtualLevelScreenPipelineLifecycleManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
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
    public void staticGeometryPass(BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc renderOffset) {
        final var lookFrom = camera.getPosition().toVector3f();

        ctx.translucencyChain.prepareLayer(Gander.asResource("main"));

        BlockRenderer.renderSectionBlocks(ctx.bakedLevel, ctx.renderTypeStore, RenderType.solid(), poseStack, lookFrom, projectionMatrix);
        BlockRenderer.renderSectionFluids(ctx.bakedLevel, ctx.renderTypeStore, RenderType.solid(), poseStack, lookFrom, projectionMatrix);

        BlockRenderer.renderSectionBlocks(ctx.bakedLevel, ctx.renderTypeStore, RenderType.cutoutMipped(), poseStack, lookFrom, projectionMatrix);
        BlockRenderer.renderSectionFluids(ctx.bakedLevel, ctx.renderTypeStore, RenderType.cutoutMipped(), poseStack, lookFrom, projectionMatrix);

        BlockRenderer.renderSectionBlocks(ctx.bakedLevel, ctx.renderTypeStore, RenderType.cutout(), poseStack, lookFrom, projectionMatrix);
        BlockRenderer.renderSectionFluids(ctx.bakedLevel, ctx.renderTypeStore, RenderType.cutout(), poseStack, lookFrom, projectionMatrix);
    }

    @Override
    public void blockEntitiesPass(BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack,
                                  Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource, Vector3fc origin) {

        final var blockEntities = ctx.blockEntityPositions
            .stream()
            .map(ctx.blockAndTints::getBlockEntity)
            .filter(Objects::nonNull);

        final var lookFrom = camera.getPosition().toVector3f();

        ctx.translucencyChain.prepareLayer(Gander.asResource("entity"));

        final var mc = Minecraft.getInstance();
        final var blockEntityRenderDispatcher = mc.getBlockEntityRenderDispatcher();

        BlockEntityRender.render(ctx.blockAndTints, blockEntities, poseStack, lookFrom, ctx.renderTypeStore, bufferSource, partialTick);
    }

    private boolean isBlockEntityRendererVisible(BlockEntityRenderDispatcher dispatcher, BlockEntity blockEntity, Frustum frustum, Vector3fc origin) {
        return true;
    }

    @Override
    public void translucentGeometryPass(BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc renderOffset) {
        final var lookFrom = camera.getPosition().toVector3f();

        ctx.translucencyChain.prepareLayer(Gander.asResource("translucent"));

        BlockRenderer.renderSectionFluids(ctx.bakedLevel, ctx.renderTypeStore, RenderType.translucent(), poseStack, lookFrom, projectionMatrix);
        BlockRenderer.renderSectionBlocks(ctx.bakedLevel, ctx.renderTypeStore, RenderType.translucent(), poseStack, lookFrom, projectionMatrix);
    }
}
