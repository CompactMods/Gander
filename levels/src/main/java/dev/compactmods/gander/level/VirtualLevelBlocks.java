package dev.compactmods.gander.level;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class VirtualLevelBlocks implements BlockAndTintGetter {

	private final Long2ObjectMap<BlockState> states;
	private final Long2ObjectMap<BlockEntity> blockEntities;

	private final VirtualLightEngine lightEngine = new VirtualLightEngine(pos -> 0, pos -> 15, this);
	private final Biome PLAINS;

	public VirtualLevelBlocks() {
		this.states = new Long2ObjectOpenHashMap<>();
		this.states.defaultReturnValue(Blocks.AIR.defaultBlockState());
		this.blockEntities = new Long2ObjectOpenHashMap<>();

		this.PLAINS = Minecraft.getInstance().level.registryAccess()
				.registryOrThrow(Registries.BIOME)
				.get(Biomes.PLAINS);
	}

	public BlockState setBlockState(BlockPos pos, @NotNull BlockState state) {
		if(state.isAir())
			this.states.remove(pos.asLong());
		else
			this.states.put(pos.asLong(), state);
		this.blockEntities.remove(pos.asLong());
		return state;
	}

	public BlockEntity setBlockEntity(BlockPos pos, BlockEntity be) {
		this.blockEntities.put(pos.asLong(), be);
		return be;
	}

	@Override
	public float getShade(Direction pDirection, boolean pShade) {
		return 1f;
	}

	@Override
	public @NotNull LevelLightEngine getLightEngine() {
		return lightEngine;
	}

	@Override
	public int getBlockTint(BlockPos pos, ColorResolver colors) {
		return colors.getColor(PLAINS, pos.getX(), pos.getZ());
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pPos) {
		return blockEntities.get(pPos.asLong());
	}

	public void removeBlockEntity(BlockPos pos) {
		blockEntities.remove(pos.asLong());
	}

	@Override
	public @NotNull BlockState getBlockState(BlockPos pos) {
		var state = this.states.get(pos.asLong());
		return state != null ? state : Blocks.AIR.defaultBlockState();
	}

	@Override
	public @NotNull FluidState getFluidState(@NotNull BlockPos pos) {
		return getBlockState(pos).getFluidState();
	}

	@Override
	public int getHeight() {
		return 255;
	}

	@Override
	public int getMinBuildHeight() {
		return 0;
	}

	public void findBlocks(BiPredicate<BlockState, BlockPos> o, BiConsumer<BlockPos, BlockState> consumer) {
		states.forEach((pos, state) -> {
			if (o.test(state, BlockPos.of(pos)))
				consumer.accept(BlockPos.of(pos), state);
		});
	}
}
