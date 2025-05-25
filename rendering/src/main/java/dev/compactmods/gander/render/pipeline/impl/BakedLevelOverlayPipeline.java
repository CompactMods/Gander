package dev.compactmods.gander.render.pipeline.impl;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.RenderTypes;
import dev.compactmods.gander.render.event.GanderRenderLevelStageEvent;
import dev.compactmods.gander.render.pipeline.MultiPassRenderPipeline;
import dev.compactmods.gander.render.pipeline.PipelineHelper;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.StageAwareRenderPipeline;
import dev.compactmods.gander.render.pipeline.phase.PipelinePhaseCollection;
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

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import net.neoforged.neoforge.common.NeoForge;

import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public record BakedLevelOverlayPipeline(PipelinePhaseCollection phaseCollection) implements StageAwareRenderPipeline, MultiPassRenderPipeline {

    private static final Predicate<RenderType> IS_TRANSLUCENT = renderType -> renderType == RenderType.TRANSLUCENT;
    private static final Set<RenderType> STATIC_GEOMETRY = Set.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout());

    public static BakedLevelOverlayPipeline INSTANCE;

    static {
        final var phases = new PipelinePhaseCollection.Builder()
            .addSetupPhase(GanderRenderToolkit::makeDeltaTracker)
            .addGeometryUploadPhase(STATIC_GEOMETRY::contains, BakedLevelOverlayPipeline::staticGeometryPass)
            .addGeometryUploadPhase(BakedLevelOverlayPipeline.IS_TRANSLUCENT, BakedLevelOverlayPipeline::blockEntitiesPass)
            .addGeometryUploadPhase(BakedLevelOverlayPipeline.IS_TRANSLUCENT, BakedLevelOverlayPipeline::translucentGeometryPass)
            .build();

        INSTANCE = new BakedLevelOverlayPipeline(phases);
    }

    @Override
    public void renderPass(PipelineState state, RenderType renderType, GuiGraphics graphics) {
        for (var preRenderPhase : phaseCollection.beforeGeometryPhases())
            preRenderPhase.run(state);

        for (var phase : phaseCollection.geometryUploadPhases()) {
            if(phase.shouldRun(renderType))
                phase.upload(state, graphics);
        }

        for (var phase : phaseCollection.renderPhases())
            phase.render(state, graphics);

        for (var phase : phaseCollection.cleanupPhases())
            phase.run(state);
    }

    @Override
    public PipelineState setup(Consumer<PipelineState> initializer) {
        final var state = new PipelineState();
        initializer.accept(state);

        if (!PipelineHelper.runStandardPipelineSetup(phaseCollection, state))
            throw new RuntimeException("Failed to setup pipeline");

        return state;
    }

    @Override
    public void handleStageEvent(RenderLevelStageEvent event, PipelineState state) {
        final var graphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());

        final var stage = event.getStage();
        final var renderTypeForStage = RenderTypes.GEOMETRY_STAGES.get(stage);

        state.set(GanderRenderToolkit.CAMERA, event.getCamera());
        state.set(GanderRenderToolkit.LEVEL_RENDERER, event.getLevelRenderer());
        state.set(GanderRenderToolkit.POSE_STACK, event.getPoseStack());
        state.set(GanderRenderToolkit.PROJECTION_MATRIX, event.getProjectionMatrix());
        state.set(GanderRenderToolkit.MODEL_VIEW_MATRIX, event.getModelViewMatrix());
        state.set(GanderRenderToolkit.CULLING_FRUSTUM, event.getFrustum());

        if (renderTypeForStage != null) {
            renderPass(state, renderTypeForStage, graphics);
        }

        var evt = GanderRenderLevelStageEvent.make(stage, state);
        NeoForge.EVENT_BUS.post(evt);
    }

    private static void staticGeometryPass(PipelineState state, GuiGraphics graphics) {
        final var deltaTracker = state.get(GanderRenderToolkit.DELTA_TRACKER);
        final var partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);

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
            for (var section : bakedLevel.sections().values()) {
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

    public static void blockEntitiesPass(PipelineState state, GuiGraphics graphics) {
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var camPos = camera.getPosition().toVector3f();
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);
        final var blockEntities = state.get(GanderRenderToolkit.BLOCK_ENTITY_POSITIONS);

        final var deltaTracker = state.get(GanderRenderToolkit.DELTA_TRACKER);
        final var partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);

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

    public static void translucentGeometryPass(PipelineState state, GuiGraphics graphics) {
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var deltaTracker = state.get(GanderRenderToolkit.DELTA_TRACKER);
        final var partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);

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
