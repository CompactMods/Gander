package dev.compactmods.gander.level;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Iterator;
import java.util.stream.Stream;

@FunctionalInterface
public interface BlockEntityResolver {

	Stream<BlockEntity> getBlockEntities(BoundedBlockAndTintGetter level);
}
