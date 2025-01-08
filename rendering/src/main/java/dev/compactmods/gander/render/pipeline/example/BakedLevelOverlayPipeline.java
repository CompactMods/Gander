package dev.compactmods.gander.render.pipeline.example;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.core.camera.MovableCamera;
import dev.compactmods.gander.render.RenderTypes;
import dev.compactmods.gander.render.pipeline.MultiPassRenderPipeline;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.context.BakedDirectLevelRenderingContext;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
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

import java.util.Set;
import java.util.function.Function;

public final class BakedLevelOverlayPipeline implements MultiPassRenderPipeline<BakedDirectLevelRenderingContext> {

    public static final BakedLevelOverlayPipeline INSTANCE = new BakedLevelOverlayPipeline();

    public static final PipelineState.Item<Vector3fc> RENDER_OFFSET = new PipelineState.Item<>(Vector3fc.class);

    private final Set<RenderType> STATIC_GEOMETRY = Set.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout());

    // @Override
    public void staticGeometryPass(PipelineState state, BakedDirectLevelRenderingContext ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc origin) {

        var renderOrigin = new Vector3f(state.getOrDefault(RENDER_OFFSET, new Vector3f()));

        final var camPos = camera.getPosition();

        // FIXME - This translation is wrong, it glues the render to the top of the player's head
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        poseStack.translate(renderOrigin.x, renderOrigin.y, renderOrigin.z);

        poseStack.pushPose();

        for (RenderType renderType : STATIC_GEOMETRY) {
            BlockRenderer.renderSectionLayer(
                ctx.blockBuffers(),
                Function.identity(),
                renderType,
                poseStack,
                renderOrigin,
                projectionMatrix);

            BlockRenderer.renderSectionLayer(
                ctx.fluidBuffers(),
                Function.identity(),
                renderType,
                poseStack,
                renderOrigin,
                projectionMatrix);
        }

        poseStack.popPose();
        poseStack.popPose();
    }

    // @Override
    public void blockEntitiesPass(PipelineState state, BakedDirectLevelRenderingContext ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Frustum frustum, MultiBufferSource.BufferSource bufferSource) {
        var renderOrigin = state.getOrDefault(RENDER_OFFSET, new Vector3f());

        // Rebase the camera so that block entities get coordinates relative to their inner level, rather than the real level
//        movableCamera.setup(camera.getEntity().level(), camera.getEntity(), camera.isDetached(), false, partialTick);
//        movableCamera.moveWorldSpace(-renderOrigin.x(), -renderOrigin.y(), -renderOrigin.z());

        final var mc = Minecraft.getInstance();
        final var blockEntityRenderDispatcher = mc.getBlockEntityRenderDispatcher();

        // TODO: maybe we should raycast in the virtual level for these, rather than pulling from the real level?
//        mc.getEntityRenderDispatcher().prepare(camera.getEntity().level(), movableCamera, Minecraft.getInstance().crosshairPickEntity);
        blockEntityRenderDispatcher.prepare(ctx.level().originalLevel(), camera, mc.hitResult);

        final var camPos = camera.getPosition();
        final var renderOffset = new Vector3f(
            (float) (renderOrigin.x() - camPos.x),
            (float) (renderOrigin.y() - camPos.y),
            (float) (renderOrigin.z() - camPos.z));

        poseStack.pushPose();
        poseStack.translate(renderOffset.x, renderOffset.y, renderOffset.z);
        ctx.blockEntities().get().forEach(blockEnt ->
            renderSingleBlockEntity(partialTick, poseStack, frustum, bufferSource, blockEnt, blockEntityRenderDispatcher, renderOrigin));

        poseStack.popPose();
    }

    private void renderSingleBlockEntity(float partialTick, PoseStack poseStack, Frustum frustum, MultiBufferSource.BufferSource bufferSource, BlockEntity blockEnt, BlockEntityRenderDispatcher blockEntityRenderDispatcher, Vector3fc renderOrigin) {
        if (!isBlockEntityRendererVisible(blockEntityRenderDispatcher, blockEnt, frustum, renderOrigin)) return;

        poseStack.pushPose();
        final var offset = Vec3.atLowerCornerOf(blockEnt.getBlockPos());
        poseStack.translate(offset.x, offset.y, offset.z);
        blockEntityRenderDispatcher.render(blockEnt, partialTick, poseStack, bufferSource);
        poseStack.popPose();
    }

    private boolean isBlockEntityRendererVisible(BlockEntityRenderDispatcher dispatcher, BlockEntity blockEntity, Frustum frustum, Vector3fc origin) {
        var renderer = dispatcher.getRenderer(blockEntity);
        return renderer != null && frustum.isVisible(renderer.getRenderBoundingBox(blockEntity).move(origin.x(), origin.y(), origin.z()));
    }

    // @Override
    public void translucentGeometryPass(BakedDirectLevelRenderingContext ctx, GuiGraphics graphics, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, Vector3fc renderOffset) {
        BlockRenderer.renderSectionLayer(
            ctx.fluidBuffers(),
            Function.identity(),
            RenderType.translucent(),
            poseStack,
            camera.getPosition().toVector3f(),
            projectionMatrix);

        BlockRenderer.renderSectionLayer(
            ctx.blockBuffers(),
            Function.identity(),
            RenderType.translucent(),
            poseStack,
            camera.getPosition().toVector3f(),
            projectionMatrix);
    }

    @Override
    public void renderPass(PipelineState state, BakedDirectLevelRenderingContext ctx, RenderType renderType, GuiGraphics graphics, Camera camera, Frustum frustum, PoseStack poseStack, Matrix4f projectionMatrix) {

        var partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        var renderOffset = new Vector3f();

        if (RenderTypes.isStaticGeometryRenderType(renderType)) {
//            var stack = new PoseStack();
//            stack.mulPose(evt.getModelViewMatrix());
            staticGeometryPass(state, ctx, graphics, partialTick, poseStack, camera, projectionMatrix, renderOffset);
        }

        if (renderType == RenderType.TRANSLUCENT) {
//            var stack = new PoseStack();
//            stack.mulPose(evt.getModelViewMatrix());

            blockEntitiesPass(state, ctx, graphics, partialTick,
                poseStack,
                camera,
                frustum,
                Minecraft.getInstance().renderBuffers().bufferSource());
        }

        if (renderType == RenderType.TRANSLUCENT_MOVING_BLOCK) {
//            var stack = new PoseStack();
//            stack.mulPose(evt.getModelViewMatrix());

            translucentGeometryPass(ctx, graphics, partialTick,
                poseStack,
                camera,
                projectionMatrix,
                renderOffset);
        }
    }

    @Override
    public PipelineState setup(BakedDirectLevelRenderingContext context, Camera camera) {
        return new PipelineState();
    }
}
