package dev.compactmods.gander.render.screen;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.compactmods.gander.render.pipeline.phase.PipelineGeometryUploadPhase;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

import org.joml.Vector3f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class GanderScreenPipelinePhases {

    public static final PipelineGeometryUploadPhase BLOCK_ENTITIES_GEOMETRY_UPLOAD = GanderScreenPipelinePhases::blockEntitiesPass;

    public static void staticPass(PipelineState state) {
        final var bakedLevel = state.get(GanderRenderToolkit.BAKED_LEVEL);

        final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        // TODO 21.6 Port
    }

    private static void blockEntitiesPass(PipelineState state) {
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
}
