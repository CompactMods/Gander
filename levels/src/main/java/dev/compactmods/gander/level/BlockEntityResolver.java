package dev.compactmods.gander.level;

import java.util.stream.Stream;
import net.minecraft.world.level.block.entity.BlockEntity;

@FunctionalInterface
public interface BlockEntityResolver {

	Stream<BlockEntity> getBlockEntities(BoundedBlockAndTintGetter level);
}
