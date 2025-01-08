package dev.compactmods.gander.world;

import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.core.camera.SceneCamera;
import dev.compactmods.gander.level.TickingLevel;

import dev.compactmods.gander.render.RenderTypes;
import dev.compactmods.gander.render.pipeline.MultiPassRenderPipeline;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.pipeline.example.BakedLevelOverlayPipeline;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.RenderType;

import org.joml.Vector3f;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.pipeline.context.BakedDirectLevelRenderingContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Serves as a reference implementation of a level-in-level renderer, using a pre-built rendering pipeline.
 */
public record LevelInLevelRenderer(UUID id, PipelineState state, BakedDirectLevelRenderingContext ctx) {
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
        final var ctx = new BakedDirectLevelRenderingContext(
            level,
            level.blockRenderBuffers(), level.fluidRenderBuffers(),
            virtualLevel.blockSystem().blockAndFluidStorage()::blockEntities
        );

        final var initialState = BakedLevelOverlayPipeline.INSTANCE.setup(ctx, null);
        initialState.set(BakedLevelOverlayPipeline.RENDER_OFFSET, renderLocation);

        return new LevelInLevelRenderer(UUID.randomUUID(), initialState, ctx);
    }

    public void onRenderStage(RenderLevelStageEvent evt) {
        final var graphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());

        final var camera = new SceneCamera();

        final var renderTypeForStage = RenderTypes.GEOMETRY_STAGES.get(evt.getStage());
        if(renderTypeForStage != null) {
            var stack = new PoseStack();
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
