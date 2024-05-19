package dev.compactmods.gander.examples;

import java.util.UUID;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import dev.compactmods.gander.render.RenderPipeline;
import dev.compactmods.gander.render.RenderTypes;
import dev.compactmods.gander.render.pipeline.BakedLevelOverlayPipeline;
import dev.compactmods.gander.render.pipeline.context.BakedDirectLevelRenderingContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.render.geometry.BakedLevel;

import org.joml.Vector3f;

/**
 * Serves as a reference implementation of a level-in-level renderer, using a pre-built rendering pipeline.
 */
public record LevelInLevelRenderer(UUID id, BakedDirectLevelRenderingContext<VirtualLevel> ctx, Vector3f renderOffset) {
    private static final Supplier<RenderPipeline<VirtualLevel>> PIPELINE = Suppliers.memoize(BakedLevelOverlayPipeline::new);

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
        final var ctx = new BakedDirectLevelRenderingContext<>(
            virtualLevel,
            level.blockRenderBuffers(), level.fluidRenderBuffers(),
            virtualLevel.blockSystem().blockAndFluidStorage()::blockEntities
        );

        return new LevelInLevelRenderer(UUID.randomUUID(), ctx, renderLocation);
    }

    public void onRenderStage(RenderLevelStageEvent evt) {
        final var pipeline = PIPELINE.get();
        if (pipeline == null) return;

        var chunkRenderType = RenderTypes.renderTypeForStage(evt.getStage());
        if (chunkRenderType != null) {
            var stack = new PoseStack();
            stack.mulPose(evt.getModelViewMatrix());

            pipeline.staticGeometryPass(ctx, chunkRenderType, stack, evt.getCamera(), evt.getProjectionMatrix(), renderOffset);
        } else if (evt.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            pipeline.blockEntitiesPass(ctx, evt.getPartialTick(),
                evt.getPoseStack(),
                evt.getCamera(),
                evt.getFrustum(),
                Minecraft.getInstance().renderBuffers().bufferSource(),
                renderOffset);
        }
    }
}
