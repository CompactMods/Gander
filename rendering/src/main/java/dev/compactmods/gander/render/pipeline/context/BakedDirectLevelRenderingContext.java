package dev.compactmods.gander.render.pipeline.context;

import com.mojang.blaze3d.vertex.VertexBuffer;

import dev.compactmods.gander.render.geometry.BakedLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Used for rendering baked level geometry directly to another level, with no render type
 * redirection being applied.
 *
 * @param level         Baked level geometry.
 * @param blockBuffers  Baked level geometry - block buffer information.
 * @param fluidBuffers  Baked level geometry - fluid buffer information.
 * @param blockEntities Supplier for the block entity information.
 */
public record BakedDirectLevelRenderingContext(BakedLevel level,
                                               Map<RenderType, VertexBuffer> blockBuffers,
                                               Map<RenderType, VertexBuffer> fluidBuffers,
                                               Supplier<Stream<BlockEntity>> blockEntities)
    implements LevelRenderingContext {
}
