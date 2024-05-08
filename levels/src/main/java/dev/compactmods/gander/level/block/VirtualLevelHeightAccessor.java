package dev.compactmods.gander.level.block;

import net.minecraft.world.level.LevelHeightAccessor;

public class VirtualLevelHeightAccessor implements LevelHeightAccessor {

	@Override
	public int getHeight() {
		return 255;
	}

	@Override
	public int getMinBuildHeight() {
		return 0;
	}
}
