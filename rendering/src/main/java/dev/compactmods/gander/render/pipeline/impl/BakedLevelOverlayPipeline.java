package dev.compactmods.gander.render.pipeline.impl;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.MultiPassRenderPipeline;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.RenderPipelineBuilder;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Objects;

public final class BakedLevelOverlayPipeline {

    public static MultiPassRenderPipeline INSTANCE;
    static {
        var builder = new RenderPipelineBuilder();
        builder.phases()
            .addGeometryUploadPhase(BakedLevelOverlayPipeline::staticGeometryPass)
            .addGeometryUploadPhase(BakedLevelOverlayPipeline::blockEntitiesPass);

        INSTANCE = builder.stagedMultiPass();
    }

    private static void staticGeometryPass(PipelineState state) {
//        var renderOrigin = getCorrectedRenderOrigin(state, partialTicks);
        final var camera = state.get(GanderRenderToolkit.CAMERA);

        final var camPos = camera.getPosition().toVector3f();
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);

        final var projectionMatrix = state.get(GanderRenderToolkit.PROJECTION_MATRIX);
        final var modelViewMatrix = state.get(GanderRenderToolkit.MODEL_VIEW_MATRIX);

        // TODO 21.6 Port - Static Geometry Overlay Phase
//        for (RenderType renderType : STATIC_GEOMETRY) {
//            for(var section : bakedLevel.sections().values()) {
//                BlockRenderer.renderSectionLayer(
//                    section.gpuBuffers(),
//                    Function.identity(),
//                    renderType,
//                    poseStack,
//                    camPos, renderOrigin,
//                    projectionMatrix);
//
//                BlockRenderer.renderSectionLayer(
//                    section.fluidBuffers(),
//                    Function.identity(),
//                    renderType,
//                    poseStack,
//                    camPos, renderOrigin,
//                    projectionMatrix);
//            }
//        }
    }

    // TODO: Check F5 View Mode
    private static Vector3f getCorrectedRenderOrigin(PipelineState state) {
        final var mc = Minecraft.getInstance();
        final var partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        var origin = new Vector3f(state.getOrDefault(GanderRenderToolkit.RENDER_ORIGIN, new Vector3f()));
        var player = Objects.requireNonNull(mc.player);
        return new Vector3f(
            Mth.lerp(partialTicks, (float) (origin.x() - player.xOld), (float) (origin.x() - player.getX())),
            Mth.lerp(partialTicks, (float) (origin.y() - player.yOld), (float) (origin.y() - player.getY())),
            Mth.lerp(partialTicks, (float) (origin.z() - player.zOld), (float) (origin.z() - player.getZ()))
        );
    }

    public static void blockEntitiesPass(PipelineState state) {
        final var mc = Minecraft.getInstance();
        final var partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var camPos = camera.getPosition().toVector3f();
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);
        final var blockEntities = state.get(GanderRenderToolkit.BLOCK_ENTITY_POSITIONS);

        var renderOrigin = getCorrectedRenderOrigin(state);

        final var bufferSource = mc.renderBuffers().bufferSource();
        final var blockEntityRenderDispatcher = mc.getBlockEntityRenderDispatcher();
        blockEntityRenderDispatcher.prepare(camera);

        final var renderOffset = new Vector3f(renderOrigin).sub(camPos);

        final var poseStack = new PoseStack();
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
//        poseStack.pushPose();
//        final var offset = Vec3.atLowerCornerOf(blockEnt.getBlockPos());
//        poseStack.translate(offset.x, offset.y, offset.z);
//        blockEntityRenderDispatcher.getRenderer(blockEnt)
//            .submit(partialTick, poseStack, bufferSource);
//        poseStack.popPose();
    }

}
