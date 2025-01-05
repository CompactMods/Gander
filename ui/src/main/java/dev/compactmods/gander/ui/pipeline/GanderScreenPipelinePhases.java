package dev.compactmods.gander.ui.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.render.pipeline.PipelineGeometryUploadPhase;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.toolkit.BlockEntityRender;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenRenderingContext;
import dev.compactmods.gander.ui.toolkit.GanderScreenToolkit;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

import org.joml.Matrix4f;

import java.util.Objects;

public class GanderScreenPipelinePhases {

    public static final PipelineGeometryUploadPhase<BakedLevelScreenRenderingContext> STATIC_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::staticPass;
    public static final PipelineGeometryUploadPhase<BakedLevelScreenRenderingContext> BLOCK_ENTITIES_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::blockEntitiesPass;
    public static final PipelineGeometryUploadPhase<BakedLevelScreenRenderingContext> TRANSLUCENT_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::translucentPass;

    private static void staticPass(PipelineState state, BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix) {
        final var lookFrom = camera.getPosition().toVector3f();

        final var chain = state.get(GanderScreenToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderScreenToolkit.RENDER_TYPE_STORE);

        chain.prepareLayer(Gander.asResource("main"));

        BlockRenderer.renderSectionBlocks(ctx.bakedLevel(), renderTypeStore, RenderType.solid(), poseStack, lookFrom, projectionMatrix);
        BlockRenderer.renderSectionFluids(ctx.bakedLevel(), renderTypeStore, RenderType.solid(), poseStack, lookFrom, projectionMatrix);

        BlockRenderer.renderSectionBlocks(ctx.bakedLevel(), renderTypeStore, RenderType.cutoutMipped(), poseStack, lookFrom, projectionMatrix);
        BlockRenderer.renderSectionFluids(ctx.bakedLevel(), renderTypeStore, RenderType.cutoutMipped(), poseStack, lookFrom, projectionMatrix);

        BlockRenderer.renderSectionBlocks(ctx.bakedLevel(), renderTypeStore, RenderType.cutout(), poseStack, lookFrom, projectionMatrix);
        BlockRenderer.renderSectionFluids(ctx.bakedLevel(), renderTypeStore, RenderType.cutout(), poseStack, lookFrom, projectionMatrix);
    }

    private static void blockEntitiesPass(PipelineState state, BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix) {

        final var blockEntities = ctx.blockEntityPositions()
            .stream()
            .map(ctx.blockAndTints()::getBlockEntity)
            .filter(Objects::nonNull);

        final var mc = Minecraft.getInstance();
        final var lookFrom = camera.getPosition().toVector3f();
        final var chain = state.get(GanderScreenToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderScreenToolkit.RENDER_TYPE_STORE);

        final var partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);

        chain.prepareLayer(Gander.asResource("entity"));

        BlockEntityRender.render(ctx.blockAndTints(), blockEntities, poseStack, lookFrom, renderTypeStore, graphics.bufferSource(), partialTick);
    }

    private static void translucentPass(PipelineState state, BakedLevelScreenRenderingContext ctx, GuiGraphics graphics, Camera camera, PoseStack poseStack, Matrix4f projectionMatrix) {
        final var lookFrom = camera.getPosition().toVector3f();
        final var chain = state.get(GanderScreenToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderScreenToolkit.RENDER_TYPE_STORE);

        chain.prepareLayer(Gander.asResource("translucent"));

        BlockRenderer.renderSectionFluids(ctx.bakedLevel(), renderTypeStore, RenderType.translucent(), poseStack, lookFrom, projectionMatrix);
        BlockRenderer.renderSectionBlocks(ctx.bakedLevel(), renderTypeStore, RenderType.translucent(), poseStack, lookFrom, projectionMatrix);
    }
}
