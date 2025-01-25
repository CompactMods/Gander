package dev.compactmods.gander.ui.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.render.pipeline.phase.PipelineGeometryUploadPhase;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.toolkit.BlockEntityRender;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Objects;

public class GanderScreenPipelinePhases {

    public static final PipelineGeometryUploadPhase STATIC_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::staticPass;
    public static final PipelineGeometryUploadPhase BLOCK_ENTITIES_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::blockEntitiesPass;
    public static final PipelineGeometryUploadPhase TRANSLUCENT_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::translucentPass;

    private static void staticPass(PipelineState state, GuiGraphics graphics, Camera camera, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, float partialTicks) {
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);
        final var chain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderRenderToolkit.RENDER_TYPE_STORE);
        final var renderOrigin = state.getOrDefault(GanderRenderToolkit.RENDER_ORIGIN, new Vector3f());

        chain.prepareLayer(Gander.asResource("main"));

        final var camPos = camera.getPosition().toVector3f();

        BlockRenderer.renderSectionBlocks(bakedLevel, renderTypeStore, RenderType.solid(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
        BlockRenderer.renderSectionFluids(bakedLevel, renderTypeStore, RenderType.solid(), graphics.pose(), camPos, renderOrigin, projectionMatrix);

        BlockRenderer.renderSectionBlocks(bakedLevel, renderTypeStore, RenderType.cutoutMipped(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
        BlockRenderer.renderSectionFluids(bakedLevel, renderTypeStore, RenderType.cutoutMipped(), graphics.pose(), camPos, renderOrigin, projectionMatrix);

        BlockRenderer.renderSectionBlocks(bakedLevel, renderTypeStore, RenderType.cutout(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
        BlockRenderer.renderSectionFluids(bakedLevel, renderTypeStore, RenderType.cutout(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
    }

    private static void blockEntitiesPass(PipelineState state, GuiGraphics graphics, Camera camera, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, float partialTicks) {
        final var mc = Minecraft.getInstance();
        final var lookFrom = camera.getPosition().toVector3f();
        final var chain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderRenderToolkit.RENDER_TYPE_STORE);
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);

        final var blockEntityPositions = state.get(GanderRenderToolkit.BLOCK_ENTITY_POSITIONS);

        final var partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);

        final var blockEntities = Arrays.stream(blockEntityPositions)
            .map(bakedLevel.originalLevel()::getBlockEntity)
            .filter(Objects::nonNull);

        chain.prepareLayer(Gander.asResource("entity"));

        final var dispatcher = mc.getBlockEntityRenderDispatcher();
        dispatcher.prepare(bakedLevel.originalLevel(), camera, null);

        BlockEntityRender.render(bakedLevel.originalLevel(), blockEntities, graphics.pose(), lookFrom, renderTypeStore, graphics.bufferSource(), partialTick);
    }

    private static void translucentPass(PipelineState state, GuiGraphics graphics, Camera camera, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, float partialTicks) {
        final var chain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderRenderToolkit.RENDER_TYPE_STORE);
        final var renderOrigin = state.getOrDefault(GanderRenderToolkit.RENDER_ORIGIN, new Vector3f());

        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);

        chain.prepareLayer(Gander.asResource("translucent"));

        final var camPos = camera.getPosition().toVector3f();

        BlockRenderer.renderSectionFluids(bakedLevel, renderTypeStore, RenderType.translucent(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
        BlockRenderer.renderSectionBlocks(bakedLevel, renderTypeStore, RenderType.translucent(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
    }
}
