package dev.compactmods.gander.world;

import java.util.UUID;

import dev.compactmods.gander.core.camera.SceneCamera;
import dev.compactmods.gander.level.TickingLevel;

import dev.compactmods.gander.render.RenderTypes;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.impl.BakedLevelOverlayPipeline;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.core.BlockPos;

import net.minecraft.util.Mth;

import org.joml.Vector3f;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.render.geometry.BakedLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Serves as a reference implementation of a level-in-level renderer, using a pre-built rendering pipeline.
 */
public record LevelInLevelRenderer(UUID id, PipelineState state) {
    public static LevelInLevelRenderer create(BakedLevel level, VirtualLevel virtualLevel) {
        var bounds = virtualLevel.getBounds();
        final var centerBlock = BlockPos.containing(bounds.getCenter())
            .mutable()
            .setY(Mth.floor(bounds.minY))
            .immutable();

        final var centerVector = Vec3.atLowerCornerOf(centerBlock).toVector3f();

        return create(level, virtualLevel, centerVector);
    }

    public static LevelInLevelRenderer create(BakedLevel level, VirtualLevel virtualLevel, Vector3f renderLocation) {
        final var initialState = BakedLevelOverlayPipeline.INSTANCE.setup((state) -> {
            final var blockEntityPos = virtualLevel.blockSystem()
                .blockAndFluidStorage()
                .blockEntityPositions()
                .toArray(BlockPos[]::new);

            state.set(GanderRenderToolkit.BAKED_LEVEL, level);
            state.set(GanderRenderToolkit.BLOCK_ENTITY_POSITIONS, blockEntityPos);
            state.set(GanderRenderToolkit.RENDER_ORIGIN, renderLocation);
            state.set(GanderRenderToolkit.CAMERA, new SceneCamera());
        });

        return new LevelInLevelRenderer(UUID.randomUUID(), initialState);
    }

    public void onRenderStage(RenderLevelStageEvent evt) {
        final var graphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());

        final var renderTypeForStage = RenderTypes.GEOMETRY_STAGES.get(evt.getStage());

        if (renderTypeForStage != null) {
            state.set(GanderRenderToolkit.PROJECTION_MATRIX, evt.getProjectionMatrix());
            state.set(GanderRenderToolkit.MODEL_VIEW_MATRIX, evt.getModelViewMatrix());
            state.set(GanderRenderToolkit.CULLING_FRUSTUM, evt.getFrustum());

            BakedLevelOverlayPipeline.INSTANCE.renderPass(state, renderTypeForStage, graphics);
        }
    }

    public void onClientTick(ClientTickEvent.Post event) {
        final var level = state.get(GanderRenderToolkit.BAKED_LEVEL);
        if (level.originalLevel() instanceof TickingLevel vl)
            vl.tick(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
    }
}
