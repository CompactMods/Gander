package dev.compactmods.gander.utility;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class BBHelper {

	public static BoundingBox encapsulate(BoundingBox bb, BlockPos pos) {
		return new BoundingBox(Math.min(bb.minX(), pos.getX()), Math.min(bb.minY(), pos.getY()),
			Math.min(bb.minZ(), pos.getZ()), Math.max(bb.maxX(), pos.getX()), Math.max(bb.maxY(), pos.getY()),
			Math.max(bb.maxZ(), pos.getZ()));
	}

	public static BlockPos minCorner(BoundingBox aabb) {
		return new BlockPos(aabb.minX(), aabb.minY(), aabb.minZ());
	}

	public static BlockPos maxCorner(BoundingBox aabb) {
		return new BlockPos(aabb.maxX(), aabb.maxY(), aabb.maxZ());
	}


	public static Stream<ChunkPos> chunksInBounds(BoundingBox bb) {
		var minChunk = new ChunkPos(minCorner(bb));
		var maxChunk = new ChunkPos(maxCorner(bb));
		if(minChunk.equals(maxChunk))
			return Stream.of(minChunk);

		return ChunkPos.rangeClosed(minChunk, maxChunk);
	}

}
