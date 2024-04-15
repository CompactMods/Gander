package dev.compactmods.gander.ponder.level;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

import org.jetbrains.annotations.Nullable;

public class EmptyBlockAndTintGetter implements BlockAndTintGetter {

	public static final EmptyBlockAndTintGetter INSTANCE = new EmptyBlockAndTintGetter();
	public final VirtualLightEngine LIGHT_ENGINE = new VirtualLightEngine(p -> 0, p -> 15, this);

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		return EmptyBlockGetter.INSTANCE.getBlockEntity(pos);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return EmptyBlockGetter.INSTANCE.getBlockState(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return EmptyBlockGetter.INSTANCE.getFluidState(pos);
	}

	@Override
	public int getHeight() {
		return EmptyBlockGetter.INSTANCE.getHeight();
	}

	@Override
	public int getMinBuildHeight() {
		return EmptyBlockGetter.INSTANCE.getMinBuildHeight();
	}

	@Override
	public float getShade(Direction pDirection, boolean pShade) {
		return 1f;
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return LIGHT_ENGINE;
	}

	@Override
	public int getBlockTint(BlockPos pos, ColorResolver resolver) {
		// How the heck does this work? Biomes?
		var plainsBiome = Minecraft.getInstance().getConnection().registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
		return resolver.getColor(plainsBiome, pos.getX(), pos.getZ());
	}
}
