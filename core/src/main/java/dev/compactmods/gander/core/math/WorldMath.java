package dev.compactmods.gander.core.math;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldMath {

    public static ChunkPos minCornerChunk(AABB aabb) {
        var mn = BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ);
        return new ChunkPos(mn);
    }

    public static ChunkPos maxCornerChunk(AABB aabb) {
        var mx = BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ);
        return new ChunkPos(mx);
    }

    public static AABB chunkAABB(Level level, ChunkPos chunkPos) {
        var minBlock = new BlockPos(chunkPos.getMinBlockX(), level.getMinY(), chunkPos.getMinBlockZ());
        var maxBlock = new BlockPos(chunkPos.getMaxBlockX(), level.getMaxY(), chunkPos.getMaxBlockZ());
        return AABB.encapsulatingFullBlocks(minBlock, maxBlock);
    }

    public static AABB sectionABB(SectionPos sectionPos) {
        var minBlock = new BlockPos(sectionPos.minBlockX(), sectionPos.minBlockY(), sectionPos.minBlockZ());
        var maxBlock = new BlockPos(sectionPos.maxBlockX(), sectionPos.maxBlockY(), sectionPos.maxBlockZ());
        return AABB.encapsulatingFullBlocks(minBlock, maxBlock);
    }

    public static BlockPos randomPosInAABB(RandomSource random, AABB aabb) {
        int rx = random.nextIntBetweenInclusive(Mth.floor(aabb.minX), Mth.floor(aabb.maxX));
        int ry = random.nextIntBetweenInclusive(Mth.floor(aabb.minY), Mth.floor(aabb.maxY));
        int ra = random.nextIntBetweenInclusive(Mth.floor(aabb.minZ), Mth.floor(aabb.maxZ));
        return new BlockPos(rx, ry, ra);
    }

    public static Stream<ChunkPos> chunkPositions(AABB aabb) {
        return ChunkPos.rangeClosed(minCornerChunk(aabb), maxCornerChunk(aabb));
    }

    public static Stream<SectionPos> sectionPositions(Level level, AABB area) {
        final var minChunk = WorldMath.minCornerChunk(area);
        final var maxChunk = WorldMath.maxCornerChunk(area);
        return ChunkPos.rangeClosed(minChunk, maxChunk)
            .mapMulti((ChunkPos cp, Consumer<SectionPos> nums) -> {
                for (int y = level.getMinSectionY(); y <= level.getMaxSectionY(); ++y)
                    nums.accept(SectionPos.of(cp, y));
            });
    }
}
