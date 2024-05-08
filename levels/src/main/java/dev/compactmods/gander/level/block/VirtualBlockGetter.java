package dev.compactmods.gander.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VirtualBlockGetter implements BlockGetter {

	private final VirtualBlockAndFluidStorage storage;
	private final LevelHeightAccessor heightAccessor;

	public VirtualBlockGetter(LevelHeightAccessor heightAccessor, VirtualBlockAndFluidStorage storage) {
		this.storage = storage;
		this.heightAccessor = heightAccessor;
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(@NotNull BlockPos pos) {
		return storage.getBlockEntity(pos);
	}

	public void removeBlockEntity(BlockPos pos) {
		storage.removeBlockEntity(pos);
	}

	@Override
	public @NotNull BlockState getBlockState(@NotNull BlockPos pos) {
		return storage.getBlockState(pos);
	}

	@Override
	public @NotNull FluidState getFluidState(@NotNull BlockPos pos) {
		return storage.getFluidState(pos);
	}

	//region LevelHeightAccessor
	@Override
	public int getHeight() {
		return heightAccessor.getHeight();
	}

	@Override
	public int getMinBuildHeight() {
		return heightAccessor.getMinBuildHeight();
	}
	//endregion
}
