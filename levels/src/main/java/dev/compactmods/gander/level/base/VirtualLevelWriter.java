package dev.compactmods.gander.level.base;

import org.jetbrains.annotations.Nullable;

import dev.compactmods.gander.level.block.VirtualBlockAndFluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.state.BlockState;

public class VirtualLevelWriter implements LevelWriter {

	private final VirtualBlockAndFluidStorage storage;

	public VirtualLevelWriter(VirtualBlockAndFluidStorage storage) {
		this.storage = storage;
	}

	@Override
	public boolean setBlock(BlockPos pPos, BlockState pState, int pFlags, int pRecursionLeft) {
		storage.setBlockState(pPos, pState);
		return true;
	}

	@Override
	public boolean setBlock(BlockPos pPos, BlockState pNewState, int pFlags) {
		return LevelWriter.super.setBlock(pPos, pNewState, pFlags);
	}

	@Override
	public boolean removeBlock(BlockPos pPos, boolean pIsMoving) {
		return false;
	}

	@Override
	public boolean destroyBlock(BlockPos pPos, boolean pDropBlock, @Nullable Entity pEntity, int pRecursionLeft) {
		return false;
	}

	@Override
	public boolean addFreshEntity(Entity pEntity) {
		return LevelWriter.super.addFreshEntity(pEntity);
	}
}
