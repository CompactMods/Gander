package dev.compactmods.gander.utility;

import dev.compactmods.gander.level.BlockEntityResolver;
import dev.compactmods.gander.level.BoundedBlockAndTintGetter;

public class BoundedLevelHelper {

	public static final BlockEntityResolver BLOCK_ENTITY_RESOLVER = (BoundedBlockAndTintGetter level) ->
			BBHelper.chunksInBounds(level.bounds())
					.map(p -> level.getChunk(p.x, p.z))
					.flatMap(chunk -> chunk.getBlockEntities().values().stream());

}
