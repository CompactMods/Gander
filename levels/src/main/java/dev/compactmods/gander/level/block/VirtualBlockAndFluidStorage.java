package dev.compactmods.gander.level.block;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

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
        var blockEntity = blockEntities.remove(pos.asLong());

        if(blockEntity != null)
            blockEntity.setRemoved();
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
        var oldBlockState = getBlockState(pos);

        // same block, quit out early
        if(oldBlockState.is(state.getBlock()))
            return oldBlockState;

        var longPos = pos.asLong();
        // notify old block of removal
        oldBlockState.onRemove(owningLevel, pos, state, false);

        // store new block state (remove if air)
        if(state.isAir())
            states.remove(longPos);
        else
            states.put(longPos, state);

        // notify new block of placement
        state.onPlace(owningLevel, pos, oldBlockState, false);

        // update stored block entity
        removeBlockEntity(pos); // remove current block entity

        if(state.hasBlockEntity()) {
            var blockEntity = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);

            if(blockEntity != null)
                setBlockEntity(blockEntity);
        }

        return state;
	}

	public boolean setBlock(BlockPos pos, BlockState state, int pFlags, int pRecursionLeft) {
		setBlockState(pos, state);
		return true;
	}

	public void setBlockEntity(BlockEntity blockEntity) {
        removeBlockEntity(blockEntity.getBlockPos()); // remove current block entity

        if(blockEntity.getBlockState().hasBlockEntity()) {
            blockEntity.setLevel(owningLevel);
            blockEntity.clearRemoved();
            blockEntities.put(blockEntity.getBlockPos().asLong(), blockEntity);
        }
	}

	public Stream<BlockPos> blockEntityPositions() {
		return blockEntities.keySet()
				.longStream()
				.mapToObj(BlockPos::of);
	}

    public Stream<BlockEntity> blockEntities() {
        return blockEntities.values().stream();
    }
}
