package dev.compactmods.gander.render.pipeline.impl;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.MultiPassRenderPipeline;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.RenderPipelineBuilder;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BakedLevelOverlayPipeline {

    private static final Predicate<RenderType> IS_TRANSLUCENT = renderType -> renderType == RenderType.TRANSLUCENT;
    private static final Set<RenderType> STATIC_GEOMETRY = Set.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout());

    public static MultiPassRenderPipeline INSTANCE;
    static {
        var builder = new RenderPipelineBuilder();
        builder.phases()
            .addGeometryUploadPhase(STATIC_GEOMETRY::contains, BakedLevelOverlayPipeline::staticGeometryPass)
            .addGeometryUploadPhase(BakedLevelOverlayPipeline.IS_TRANSLUCENT, BakedLevelOverlayPipeline::blockEntitiesPass)
            .addGeometryUploadPhase(BakedLevelOverlayPipeline.IS_TRANSLUCENT, BakedLevelOverlayPipeline::translucentGeometryPass);

        INSTANCE = builder.stagedMultiPass();
    }

    private static void staticGeometryPass(PipelineState state, GuiGraphics graphics, float partialTicks) {
        var renderOrigin = getCorrectedRenderOrigin(state, partialTicks);
        final var camera = state.get(GanderRenderToolkit.CAMERA);

        final var camPos = camera.getPosition().toVector3f();
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);

        final var projectionMatrix = state.get(GanderRenderToolkit.PROJECTION_MATRIX);
        final var modelViewMatrix = state.get(GanderRenderToolkit.MODEL_VIEW_MATRIX);

        final var poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.mulPose(modelViewMatrix);

        for (RenderType renderType : STATIC_GEOMETRY) {
            for(var section : bakedLevel.sections().values()) {
                BlockRenderer.renderSectionLayer(
                    section.blockBuffers(),
                    Function.identity(),
                    renderType,
                    poseStack,
                    camPos, renderOrigin,
                    projectionMatrix);

                BlockRenderer.renderSectionLayer(
                    section.fluidBuffers(),
                    Function.identity(),
                    renderType,
                    poseStack,
                    camPos, renderOrigin,
                    projectionMatrix);
            }
        }

        poseStack.popPose();
    }


    private static Vector3f getCorrectedRenderOrigin(PipelineState state, float partialTicks) {
        var origin = new Vector3f(state.getOrDefault(GanderRenderToolkit.RENDER_ORIGIN, new Vector3f()));
        var player = Objects.requireNonNull(Minecraft.getInstance().player);
        return new Vector3f(
            Mth.lerp(partialTicks, (float) (origin.x() - player.xOld), (float) (origin.x() - player.getX())),
            Mth.lerp(partialTicks, (float) (origin.y() - player.yOld), (float) (origin.y() - player.getY())),
            Mth.lerp(partialTicks, (float) (origin.z() - player.zOld), (float) (origin.z() - player.getZ()))
        );
    }

    public static void blockEntitiesPass(PipelineState state, GuiGraphics graphics, float partialTicks) {

        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var camPos = camera.getPosition().toVector3f();
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);
        final var blockEntities = state.get(GanderRenderToolkit.BLOCK_ENTITY_POSITIONS);

        var renderOrigin = getCorrectedRenderOrigin(state, partialTicks);

        final var mc = Minecraft.getInstance();
        final var bufferSource = mc.renderBuffers().bufferSource();
        final var blockEntityRenderDispatcher = mc.getBlockEntityRenderDispatcher();

        // TODO: maybe we should raycast in the virtual level for these, rather than pulling from the real level?
        blockEntityRenderDispatcher.prepare(bakedLevel.originalLevel(), camera, mc.hitResult);

        final var renderOffset = new Vector3f(renderOrigin).sub(camPos);

        final var poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(renderOffset.x, renderOffset.y, renderOffset.z);
        Arrays.stream(blockEntities)
            .map(bakedLevel.originalLevel()::getBlockEntity)
            .forEach(blockEnt -> {
                renderSingleBlockEntity(partialTicks, poseStack, bufferSource, blockEnt, blockEntityRenderDispatcher);
            });

        poseStack.popPose();
    }

    private static void renderSingleBlockEntity(float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                         BlockEntity blockEnt, BlockEntityRenderDispatcher blockEntityRenderDispatcher) {
        poseStack.pushPose();
        final var offset = Vec3.atLowerCornerOf(blockEnt.getBlockPos());
        poseStack.translate(offset.x, offset.y, offset.z);
        blockEntityRenderDispatcher.render(blockEnt, partialTick, poseStack, bufferSource);
        poseStack.popPose();
    }

    public static void translucentGeometryPass(PipelineState state, GuiGraphics graphics, float partialTicks) {
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        var renderOrigin = getCorrectedRenderOrigin(state, partialTicks);
        final var camPos = camera.getPosition().toVector3f();
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);
        final var projectionMatrix = state.get(GanderRenderToolkit.PROJECTION_MATRIX);
        final var modelViewMatrix = state.get(GanderRenderToolkit.MODEL_VIEW_MATRIX);

        final var poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.mulPose(modelViewMatrix);

        bakedLevel.sections().forEach((chunkPos, section) -> {
            BlockRenderer.renderSectionLayer(
                section.fluidBuffers(),
                Function.identity(),
                RenderType.translucent(),
                poseStack,
                camPos, renderOrigin,
                projectionMatrix);

            BlockRenderer.renderSectionLayer(
                section.blockBuffers(),
                Function.identity(),
                RenderType.translucent(),
                poseStack,
                camPos, renderOrigin,
                projectionMatrix);
        });

        poseStack.pushPose();
    }
}
