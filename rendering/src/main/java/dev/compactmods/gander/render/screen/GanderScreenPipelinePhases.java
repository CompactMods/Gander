package dev.compactmods.gander.render.screen;

import java.util.Arrays;
import java.util.Objects;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.phase.PipelineGeometryUploadPhase;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;

public class GanderScreenPipelinePhases {

    public static final PipelineGeometryUploadPhase BLOCK_ENTITIES_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::blockEntitiesPass;

    public static void staticPass(PipelineState state) {
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);
        final var camera = state.get(GanderRenderToolkit.CAMERA);

        final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        // TODO 21.6 Port
        for (var section : bakedLevel.sections().entrySet()) {
            TranslucencyPointOfView tpov = TranslucencyPointOfView.of(camera.getPosition(), section.getKey());
            final var bakedSection = section.getValue();

            try(final var compiledMesh = new CompiledSectionMesh(tpov, bakedSection.layers())) {
                bakedSection.layers().renderedLayers.forEach((layer, mesh) -> {
                    compiledMesh.uploadMeshLayer(layer, mesh, section.getKey());
//                    mesh.close();
                });
            }
        }
    }

    private static void blockEntitiesPass(PipelineState state) {
        final var mc = Minecraft.getInstance();
        final var bufferSource = mc.renderBuffers().bufferSource();
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var lookFrom = camera.getPosition().toVector3f();
        // final var chain = state.get(GanderRenderToolkit.TRANSLUCENCY_CHAIN);
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);

        final var blockEntityPositions = state.get(GanderRenderToolkit.BLOCK_ENTITY_POSITIONS);

        final var partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        final var blockEntities = Arrays.stream(blockEntityPositions)
            .map(bakedLevel.originalLevel()::getBlockEntity)
            .filter(Objects::nonNull);

//        chain.prepareLayer(Gander.asResource("entity"));

        final var dispatcher = mc.getBlockEntityRenderDispatcher();
        dispatcher.prepare(camera);

//        BlockEntityRender.render(bakedLevel.originalLevel(), blockEntities, graphics.pose(), lookFrom, renderTypeStore, bufferSource, partialTick);
    }
}
