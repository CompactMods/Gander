package dev.compactmods.gander.level;

import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.stream.Stream;

public interface BoundedBlockAndTintGetter extends BlockAndTintGetter {

	Stream<BlockEntity> blockEntities();

	BoundingBox bounds();

}
