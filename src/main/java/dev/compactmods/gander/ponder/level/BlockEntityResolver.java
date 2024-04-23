package dev.compactmods.gander.ponder.level;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Iterator;

@FunctionalInterface
public interface BlockEntityResolver {

	Iterator<BlockEntity> getBlockEntities();
}
