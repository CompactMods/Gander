package dev.compactmods.gander.world;

import java.util.UUID;

import dev.compactmods.gander.core.camera.SceneCamera;
import dev.compactmods.gander.level.TickingLevel;

import dev.compactmods.gander.render.RenderTypes;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.example.BakedLevelOverlayPipeline;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Vector3f;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.render.geometry.BakedLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Serves as a reference implementation of a level-in-level renderer, using a pre-built rendering pipeline.
 */
public record LevelInLevelRenderer(UUID id, PipelineState state, BakedLevelOverlayPipeline.Context ctx) {
    public static LevelInLevelRenderer create(BakedLevel level, VirtualLevel virtualLevel) {
        BoundingBox bounds = virtualLevel.getBounds();
        final var centerBlock = bounds.getCenter()
            .multiply(-1)
            .mutable()
            .setY(bounds.minY())
            .immutable();

        final var centerVector = Vec3.atLowerCornerOf(centerBlock).toVector3f();

        return create(level, virtualLevel, centerVector);
    }

    public static LevelInLevelRenderer create(BakedLevel level, VirtualLevel virtualLevel, Vector3f renderLocation) {
        final var ctx = new BakedLevelOverlayPipeline.Context(
            level,
            level.blockRenderBuffers(), level.fluidRenderBuffers(),
            virtualLevel.blockSystem().blockAndFluidStorage()::blockEntities
        );

        final var initialState = BakedLevelOverlayPipeline.INSTANCE.setup(ctx, null);
        initialState.set(GanderRenderToolkit.RENDER_ORIGIN, renderLocation);

        return new LevelInLevelRenderer(UUID.randomUUID(), initialState, ctx);
    }

    public void onRenderStage(RenderLevelStageEvent evt) {
        final var graphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());

        final var camera = new SceneCamera();

        final var renderTypeForStage = RenderTypes.GEOMETRY_STAGES.get(evt.getStage());
        if(renderTypeForStage != null) {
            var stack = evt.getPoseStack();
            stack.mulPose(evt.getModelViewMatrix());

            BakedLevelOverlayPipeline.INSTANCE.renderPass(state, ctx, renderTypeForStage, graphics, camera,
                evt.getFrustum(),
                stack, evt.getProjectionMatrix());
        }
    }

    public void onClientTick(ClientTickEvent.Post event) {
        if(ctx.level().originalLevel() instanceof TickingLevel vl)
            vl.tick(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
    }
}
