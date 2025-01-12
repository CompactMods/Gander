package dev.compactmods.gander.ui.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.render.pipeline.phase.PipelineGeometryUploadPhase;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.toolkit.BlockEntityRender;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenRenderingContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

public class GanderScreenPipelinePhases {

    public static final PipelineGeometryUploadPhase<BakedLevelScreenRenderingContext> STATIC_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::staticPass;
    public static final PipelineGeometryUploadPhase<BakedLevelScreenRenderingContext> BLOCK_ENTITIES_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::blockEntitiesPass;
    public static final PipelineGeometryUploadPhase<BakedLevelScreenRenderingContext> TRANSLUCENT_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::translucentPass;

    private static void staticPass(PipelineState state, BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix) {
        final var chain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderRenderToolkit.RENDER_TYPE_STORE);
        final var renderOrigin = state.getOrDefault(GanderRenderToolkit.RENDER_ORIGIN, new Vector3f());

        chain.prepareLayer(Gander.asResource("main"));

        final var camPos = camera.getPosition().toVector3f();

        BlockRenderer.renderSectionBlocks(ctx.bakedLevel(), renderTypeStore, RenderType.solid(), poseStack, camPos, renderOrigin, projectionMatrix);
        BlockRenderer.renderSectionFluids(ctx.bakedLevel(), renderTypeStore, RenderType.solid(), poseStack, camPos, renderOrigin, projectionMatrix);

        BlockRenderer.renderSectionBlocks(ctx.bakedLevel(), renderTypeStore, RenderType.cutoutMipped(), poseStack, camPos, renderOrigin, projectionMatrix);
        BlockRenderer.renderSectionFluids(ctx.bakedLevel(), renderTypeStore, RenderType.cutoutMipped(), poseStack, camPos, renderOrigin, projectionMatrix);

        BlockRenderer.renderSectionBlocks(ctx.bakedLevel(), renderTypeStore, RenderType.cutout(), poseStack, camPos, renderOrigin, projectionMatrix);
        BlockRenderer.renderSectionFluids(ctx.bakedLevel(), renderTypeStore, RenderType.cutout(), poseStack, camPos, renderOrigin, projectionMatrix);
    }

    private static void blockEntitiesPass(PipelineState state, BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix) {

        final var blockEntities = ctx.blockEntityPositions()
            .stream()
            .map(ctx.blockAndTints()::getBlockEntity)
            .filter(Objects::nonNull);

        final var mc = Minecraft.getInstance();
        final var lookFrom = camera.getPosition().toVector3f();
        final var chain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderRenderToolkit.RENDER_TYPE_STORE);

        final var partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);

        chain.prepareLayer(Gander.asResource("entity"));

        BlockEntityRender.render(ctx.blockAndTints(), blockEntities, poseStack, lookFrom, renderTypeStore, graphics.bufferSource(), partialTick);
    }

    private static void translucentPass(PipelineState state, BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix) {
        final var chain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderRenderToolkit.RENDER_TYPE_STORE);
        final var renderOrigin = state.getOrDefault(GanderRenderToolkit.RENDER_ORIGIN, new Vector3f());

        chain.prepareLayer(Gander.asResource("translucent"));

        final var camPos = camera.getPosition().toVector3f();

        BlockRenderer.renderSectionFluids(ctx.bakedLevel(), renderTypeStore, RenderType.translucent(), poseStack, camPos, renderOrigin, projectionMatrix);
        BlockRenderer.renderSectionBlocks(ctx.bakedLevel(), renderTypeStore, RenderType.translucent(), poseStack, camPos, renderOrigin, projectionMatrix);
    }
}
