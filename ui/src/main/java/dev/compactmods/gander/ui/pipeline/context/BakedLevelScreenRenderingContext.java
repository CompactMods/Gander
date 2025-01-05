package dev.compactmods.gander.ui.pipeline.context;

import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.pipeline.context.LevelRenderingContext;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Set;
import java.util.stream.Collectors;

public record BakedLevelScreenRenderingContext(BakedLevel bakedLevel, BlockAndTintGetter blockAndTints, BoundingBox blockBoundaries, Set<BlockPos> blockEntityPositions)
    implements LevelRenderingContext {

    public static BakedLevelScreenRenderingContext forBakedLevel(BakedLevel bakedLevel) {
        final var blockEntityPositions = BlockPos.betweenClosedStream(bakedLevel.blockBoundaries())
            .filter(p -> bakedLevel.originalLevel().getBlockState(p).hasBlockEntity())
            .map(BlockPos::immutable)
            .collect(Collectors.toUnmodifiableSet());

        return new BakedLevelScreenRenderingContext(bakedLevel, bakedLevel.originalLevel(), bakedLevel.blockBoundaries(), blockEntityPositions);
    }

    public void recalculateTranslucency(Camera camera) {
        if(bakedLevel != null) {
            bakedLevel.resortTranslucency(camera.getPosition().toVector3f());
        }
    }
}
