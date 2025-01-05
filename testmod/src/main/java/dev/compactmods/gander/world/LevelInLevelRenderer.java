package dev.compactmods.gander.world;

import java.util.UUID;
import java.util.function.Supplier;

import dev.compactmods.gander.level.TickingLevel;
import net.minecraft.client.gui.GuiGraphics;

import org.joml.Vector3f;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.render.pipeline.RenderPipeline;
import dev.compactmods.gander.render.RenderTypes;
import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.pipeline.BakedLevelOverlayPipeline;
import dev.compactmods.gander.render.pipeline.context.BakedDirectLevelRenderingContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Serves as a reference implementation of a level-in-level renderer, using a pre-built rendering pipeline.
 */
public record LevelInLevelRenderer(UUID id, BakedDirectLevelRenderingContext ctx, Vector3f renderOffset) {
    private static final Supplier<RenderPipeline<BakedDirectLevelRenderingContext>> PIPELINE = Suppliers.memoize(BakedLevelOverlayPipeline::new);

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

        return new LevelInLevelRenderer(UUID.randomUUID(), ctx, renderLocation);
    }

    public void onRenderStage(RenderLevelStageEvent evt) {
        final var pipeline = PIPELINE.get();
        if (pipeline == null) return;

        final var graphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());

        var partialTick = evt.getPartialTick().getGameTimeDeltaPartialTick(true);

        if(RenderTypes.isStaticGeometryStage(evt.getStage())) {
            var stack = new PoseStack();
            stack.mulPose(evt.getModelViewMatrix());

            pipeline.staticGeometryPass(ctx, graphics, partialTick, stack, evt.getCamera(), evt.getProjectionMatrix(), renderOffset);
        }

        if (evt.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            var stack = new PoseStack();
//            stack.mulPose(evt.getModelViewMatrix());

            pipeline.blockEntitiesPass(ctx, graphics, partialTick,
                stack,
                evt.getCamera(),
                evt.getFrustum(),
                Minecraft.getInstance().renderBuffers().bufferSource(),
                renderOffset);
        }

        if(evt.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            var stack = new PoseStack();
            stack.mulPose(evt.getModelViewMatrix());

            pipeline.translucentGeometryPass(ctx, graphics, partialTick,
                stack,
                evt.getCamera(),
                evt.getProjectionMatrix(),
                renderOffset);
        }
    }

    public void onClientTick(ClientTickEvent.Post event) {
        if(ctx.level().originalLevel() instanceof TickingLevel vl)
            vl.tick(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
    }
}
