package dev.compactmods.gander.level.chunk;

import com.mojang.serialization.MapCodec;

import java.util.stream.IntStream;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class VirtualChunkGenerator extends ChunkGenerator {

	public VirtualChunkGenerator(BiomeSource bs) {
		super(bs);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return null;
	}

    @Override
    public void applyCarvers(
        final WorldGenRegion worldGenRegion,
        final long l,
        final RandomState randomState,
        final BiomeManager biomeManager,
        final StructureManager structureManager,
        final ChunkAccess chunkAccess)
    {

    }

	@Override
	public void buildSurface(WorldGenRegion pLevel, StructureManager pStructureManager, RandomState pRandom, ChunkAccess pChunk) {

	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion pLevel) {

	}

	@Override
	public int getGenDepth() {
		return 384;
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(
		final Blender blender,
		final RandomState randomState,
		final StructureManager structureManager,
		final ChunkAccess chunkAccess)
	{
		return CompletableFuture.completedFuture(chunkAccess);
	}


	@Override
	public int getSeaLevel() {
		return 64;
	}

	@Override
	public int getMinY() {
		return -64;
	}

	@Override
	public int getBaseHeight(int pX, int pZ, Heightmap.Types pType, LevelHeightAccessor pLevel, RandomState pRandom) {
		// return pLevel.getMinBuildHeight();
		return -63;
	}

	@Override
	public NoiseColumn getBaseColumn(int pX, int pZ, LevelHeightAccessor pHeight, RandomState pRandom) {
		return new NoiseColumn(
				pHeight.getMinY(),
				IntStream.range(0, pHeight.getHeight())
						.mapToObj(i -> Blocks.AIR.defaultBlockState())
						.toArray(BlockState[]::new)
		);
	}

	@Override
	public void addDebugScreenInfo(List<String> pInfo, RandomState pRandom, BlockPos pPos) {

	}
}
