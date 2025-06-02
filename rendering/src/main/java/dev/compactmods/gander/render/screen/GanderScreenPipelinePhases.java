package dev.compactmods.gander.render.screen;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.render.pipeline.phase.PipelineGeometryUploadPhase;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.toolkit.BlockEntityRender;
import dev.compactmods.gander.render.toolkit.BlockRenderer;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Objects;

public class GanderScreenPipelinePhases {

    public static final PipelineGeometryUploadPhase STATIC_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::staticPass;
    public static final PipelineGeometryUploadPhase BLOCK_ENTITIES_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::blockEntitiesPass;
    public static final PipelineGeometryUploadPhase TRANSLUCENT_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::translucentPass;

    private static void staticPass(PipelineState state, GuiGraphics graphics, float partialTicks) {
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);
        // final var chain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderRenderToolkit.RENDER_TYPE_STORE);
        final var renderOrigin = state.getOrDefault(GanderRenderToolkit.RENDER_ORIGIN, new Vector3f());
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var projectionMatrix = state.get(GanderRenderToolkit.PROJECTION_MATRIX);

        // chain.prepareLayer(Gander.asResource("main"));

        final var camPos = camera.getPosition().toVector3f();

        for(var section : bakedLevel.sections().values()) {
            BlockRenderer.renderSectionBlocks(section, renderTypeStore, RenderType.solid(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
            BlockRenderer.renderSectionFluids(section, renderTypeStore, RenderType.solid(), graphics.pose(), camPos, renderOrigin, projectionMatrix);

            BlockRenderer.renderSectionBlocks(section, renderTypeStore, RenderType.cutoutMipped(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
            BlockRenderer.renderSectionFluids(section, renderTypeStore, RenderType.cutoutMipped(), graphics.pose(), camPos, renderOrigin, projectionMatrix);

            BlockRenderer.renderSectionBlocks(section, renderTypeStore, RenderType.cutout(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
            BlockRenderer.renderSectionFluids(section, renderTypeStore, RenderType.cutout(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
        }
    }

    private static void blockEntitiesPass(PipelineState state, GuiGraphics graphics, float partialTicks) {
        final var mc = Minecraft.getInstance();
        final var bufferSource = mc.renderBuffers().bufferSource();
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var lookFrom = camera.getPosition().toVector3f();
        // final var chain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderRenderToolkit.RENDER_TYPE_STORE);
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);

        final var blockEntityPositions = state.get(GanderRenderToolkit.BLOCK_ENTITY_POSITIONS);

        final var partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        final var blockEntities = Arrays.stream(blockEntityPositions)
            .map(bakedLevel.originalLevel()::getBlockEntity)
            .filter(Objects::nonNull);

//        chain.prepareLayer(Gander.asResource("entity"));

        final var dispatcher = mc.getBlockEntityRenderDispatcher();
        dispatcher.prepare(bakedLevel.originalLevel(), camera, null);

//        BlockEntityRender.render(bakedLevel.originalLevel(), blockEntities, graphics.pose(), lookFrom, renderTypeStore, bufferSource, partialTick);
    }

    private static void translucentPass(PipelineState state, GuiGraphics graphics, float partialTicks) {
//        final var chain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
        final var renderTypeStore = state.get(GanderRenderToolkit.RENDER_TYPE_STORE);
        final var renderOrigin = state.getOrDefault(GanderRenderToolkit.RENDER_ORIGIN, new Vector3f());
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);

        final var projectionMatrix = state.get(GanderRenderToolkit.PROJECTION_MATRIX);

//        chain.prepareLayer(Gander.asResource("translucent"));

        final var camPos = camera.getPosition().toVector3f();

        for(var section : bakedLevel.sections().values()) {
            BlockRenderer.renderSectionFluids(section, renderTypeStore, RenderType.translucent(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
            BlockRenderer.renderSectionBlocks(section, renderTypeStore, RenderType.translucent(), graphics.pose(), camPos, renderOrigin, projectionMatrix);
        }
    }
}
