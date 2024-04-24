package dev.compactmods.gander.level;

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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class VirtualLevelBlocks implements BlockAndTintGetter {

	private final Map<BlockPos, BlockState> states;
	private final Map<BlockPos, BlockEntity> blockEntities;

	private final VirtualLightEngine lightEngine = new VirtualLightEngine(pos -> 0, pos -> 15, this);
	private final Biome PLAINS;

	public VirtualLevelBlocks() {
		this.states = new HashMap<>();
		this.blockEntities = new HashMap<>();

		this.PLAINS = Minecraft.getInstance().level.registryAccess()
				.registryOrThrow(Registries.BIOME)
				.get(Biomes.PLAINS);
	}

	public Map<BlockPos, BlockState> blocks() {
		return states;
	}

	public Map<BlockPos, BlockEntity> blockEntities() {
		return blockEntities;
	}

	@Override
	public float getShade(Direction pDirection, boolean pShade) {
		return 1f;
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return lightEngine;
	}

	@Override
	public int getBlockTint(BlockPos pos, ColorResolver colors) {
		return colors.getColor(PLAINS, pos.getX(), pos.getZ());
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pPos) {
		return blockEntities.get(pPos);
	}

	@Override
	public @NotNull BlockState getBlockState(BlockPos pos) {
		if(!states.containsKey(pos))
			return Blocks.AIR.defaultBlockState();

		return this.states.get(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
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
}
