package dev.compactmods.gander.level.block;

import com.google.common.collect.Collections2;

import dev.compactmods.gander.level.light.VirtualLightEngine;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.material.FluidState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class VirtualBlockAndFluidStorage {

	private final Long2ObjectMap<BlockState> states;
	private final Long2ObjectMap<BlockEntity> blockEntities;

	private final Level owningLevel;

	public VirtualBlockAndFluidStorage(Level owningLevel) {
		this.owningLevel = owningLevel;
		this.states = new Long2ObjectOpenHashMap<>();
		this.states.defaultReturnValue(Blocks.AIR.defaultBlockState());

		this.blockEntities = new Long2ObjectOpenHashMap<>();
	}

	@Nullable
	public BlockEntity getBlockEntity(BlockPos pPos) {
		return blockEntities.get(pPos.asLong());
	}

	public void removeBlockEntity(BlockPos pos) {
		blockEntities.remove(pos.asLong());
	}

	public @NotNull BlockState getBlockState(BlockPos pos) {
		var state = this.states.get(pos.asLong());
		return state != null ? state : Blocks.AIR.defaultBlockState();
	}

	public @NotNull FluidState getFluidState(@NotNull BlockPos pos) {
		return getBlockState(pos).getFluidState();
	}

	public void findBlocks(BiPredicate<BlockState, BlockPos> o, BiConsumer<BlockPos, BlockState> consumer) {
		states.forEach((pos, state) -> {
			if (o.test(state, BlockPos.of(pos)))
				consumer.accept(BlockPos.of(pos), state);
		});
	}

	public BlockState setBlockState(BlockPos pos, @NotNull BlockState state) {
		if (state.isAir())
			this.states.remove(pos.asLong());
		else
			this.states.put(pos.asLong(), state);
		this.blockEntities.remove(pos.asLong());
		return state;
	}

	public boolean setBlock(BlockPos pos, BlockState state, int pFlags, int pRecursionLeft) {
		setBlockState(pos, state);
		if(state.hasBlockEntity()) {
			var be = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
			if(be != null) {
				setBlockEntity(be);
				be.setLevel(owningLevel);
			}
		}

		return true;
	}

	public void setBlockEntity(BlockEntity blockEntity) {
		BlockPos blockpos = blockEntity.getBlockPos();
		if (this.getBlockState(blockpos).hasBlockEntity()) {
			blockEntity.setLevel(owningLevel);
			blockEntity.clearRemoved();
			BlockEntity blockentity = setBlockEntity(blockpos.immutable(), blockEntity);
			if (blockentity != null && blockentity != blockEntity) {
				blockentity.setRemoved();
			}
		}
	}

	public BlockEntity setBlockEntity(BlockPos pos, BlockEntity be) {
		this.blockEntities.put(pos.asLong(), be);
		return be;
	}

	public Stream<BlockPos> streamBlockEntities() {
		return blockEntities.keySet()
				.longStream()
				.mapToObj(BlockPos::of);
	}

	public Stream<BlockEntity> getBlockEntities() {
		return blockEntities.values().stream();
	}
}
