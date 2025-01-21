package dev.compactmods.gander.render.pipeline.example;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexBuffer;

import dev.compactmods.gander.render.RenderTypes;
import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.pipeline.MultiPassRenderPipeline;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.RenderPipelineBuilder;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class BakedLevelOverlayPipeline {

    private static final Predicate<RenderType> IS_TRANSLUCENT = renderType -> renderType == RenderType.TRANSLUCENT;
    private static final Set<RenderType> STATIC_GEOMETRY = Set.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout());

    public static MultiPassRenderPipeline<BakedLevelOverlayPipeline.Context> INSTANCE;
    static {
        var builder = new RenderPipelineBuilder<BakedLevelOverlayPipeline.Context>();
        builder.phases()
            .addGeometryUploadPhase(RenderTypes::isStaticGeometryRenderType, BakedLevelOverlayPipeline::staticGeometryPass)
            .addGeometryUploadPhase(BakedLevelOverlayPipeline.IS_TRANSLUCENT, BakedLevelOverlayPipeline::blockEntitiesPass)
            .addGeometryUploadPhase(BakedLevelOverlayPipeline.IS_TRANSLUCENT, BakedLevelOverlayPipeline::translucentGeometryPass);

        INSTANCE = builder.stagedMultiPass();
    }

    private static void staticGeometryPass(PipelineState state, Context ctx, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, float partialTicks) {
        var renderOrigin = getCorrectedRenderOrigin(state, partialTicks);

        final var camPos = camera.getPosition().toVector3f();

        // FIXME - This translation is wrong, it glues the render to the top of the player's head
        poseStack.pushPose();
        poseStack.mulPose(modelViewMatrix);

//        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
//        poseStack.translate(renderOrigin.x, renderOrigin.y, renderOrigin.z);

        poseStack.pushPose();

        for (RenderType renderType : STATIC_GEOMETRY) {
            BlockRenderer.renderSectionLayer(
                ctx.blockBuffers(),
                Function.identity(),
                renderType,
                poseStack,
                camPos, renderOrigin,
                projectionMatrix);

            BlockRenderer.renderSectionLayer(
                ctx.fluidBuffers(),
                Function.identity(),
                renderType,
                poseStack,
                camPos, renderOrigin,
                projectionMatrix);
        }

        poseStack.popPose();
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

    public static void blockEntitiesPass(PipelineState state, Context ctx,
                                         GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, float partialTicks) {


        final var camPos = camera.getPosition().toVector3f();
        var renderOrigin = getCorrectedRenderOrigin(state, partialTicks);

        // Rebase the camera so that block entities get coordinates relative to their inner level, rather than the real level
//        movableCamera.setup(camera.getEntity().level(), camera.getEntity(), camera.isDetached(), false, partialTick);
//        movableCamera.moveWorldSpace(-renderOrigin.x(), -renderOrigin.y(), -renderOrigin.z());

        final var mc = Minecraft.getInstance();
        final var bufferSource = mc.renderBuffers().bufferSource();
        final var blockEntityRenderDispatcher = mc.getBlockEntityRenderDispatcher();

        // TODO: maybe we should raycast in the virtual level for these, rather than pulling from the real level?
//        mc.getEntityRenderDispatcher().prepare(camera.getEntity().level(), movableCamera, Minecraft.getInstance().crosshairPickEntity);
        blockEntityRenderDispatcher.prepare(ctx.level().originalLevel(), camera, mc.hitResult);

        final var renderOffset = new Vector3f(
            (float) (renderOrigin.x() - camPos.x),
            (float) (renderOrigin.y() - camPos.y),
            (float) (renderOrigin.z() - camPos.z));

        poseStack.pushPose();
        poseStack.translate(renderOffset.x, renderOffset.y, renderOffset.z);
        ctx.blockEntities().get().forEach(blockEnt ->
            renderSingleBlockEntity(partialTicks, poseStack, bufferSource, blockEnt, blockEntityRenderDispatcher, renderOrigin));

        poseStack.popPose();
    }

    private static void renderSingleBlockEntity(float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                         BlockEntity blockEnt, BlockEntityRenderDispatcher blockEntityRenderDispatcher, Vector3fc renderOrigin) {
        poseStack.pushPose();
        final var offset = Vec3.atLowerCornerOf(blockEnt.getBlockPos());
        poseStack.translate(offset.x, offset.y, offset.z);
        blockEntityRenderDispatcher.render(blockEnt, partialTick, poseStack, bufferSource);
        poseStack.popPose();
    }

    public static void translucentGeometryPass(PipelineState state, Context ctx,
                                               GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, float partialTicks) {

        var renderOrigin = getCorrectedRenderOrigin(state, partialTicks);
        final var camPos = camera.getPosition().toVector3f();

        poseStack.pushPose();
        poseStack.mulPose(modelViewMatrix);

        BlockRenderer.renderSectionLayer(
            ctx.fluidBuffers(),
            Function.identity(),
            RenderType.translucent(),
            poseStack,
            camPos, renderOrigin,
            projectionMatrix);

        BlockRenderer.renderSectionLayer(
            ctx.blockBuffers(),
            Function.identity(),
            RenderType.translucent(),
            poseStack,
            camPos, renderOrigin,
            projectionMatrix);

        poseStack.pushPose();
    }

    /**
     * Used for rendering baked level geometry directly to another level, with no render type
     * redirection being applied.
     *
     * @param level         Baked level geometry.
     * @param blockBuffers  Baked level geometry - block buffer information.
     * @param fluidBuffers  Baked level geometry - fluid buffer information.
     * @param blockEntities Supplier for the block entity information.
     */
    public record Context(BakedLevel level,
                          Map<RenderType, VertexBuffer> blockBuffers,
                          Map<RenderType, VertexBuffer> fluidBuffers,
                          Supplier<Stream<BlockEntity>> blockEntities) {
    }
}
