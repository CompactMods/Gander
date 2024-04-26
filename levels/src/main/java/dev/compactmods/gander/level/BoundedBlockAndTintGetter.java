package dev.compactmods.gander.level;

import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public interface BoundedBlockAndTintGetter extends BlockAndTintGetter {

	BoundingBox bounds();
}
