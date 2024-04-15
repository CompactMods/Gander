package dev.compactmods.gander;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;

public interface BlockDestructionProgressExtension {
	@Nullable
	Set<BlockPos> getExtraPositions();

	void setExtraPositions(@Nullable Set<BlockPos> positions);
}
