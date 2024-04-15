package dev.compactmods.gander.ponder.level;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;

public class SchematicChunkSource extends ChunkSource {
	private final Level fallbackWorld;

	public SchematicChunkSource(Level world) {
		fallbackWorld = world;
	}

	@Nullable
	@Override
	public LightChunk getChunkForLighting(int x, int z) {
		return getChunk(x, z);
	}

	@Override
	public Level getLevel() {
		return fallbackWorld;
	}

	@Nullable
	@Override
	public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean p_212849_4_) {
		return getChunk(x, z);
	}

	public ChunkAccess getChunk(int x, int z) {
		return new EmptierChunk(fallbackWorld.registryAccess());
	}

	@Override
	public String gatherStats() {
		return "WrappedChunkProvider";
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return fallbackWorld.getLightEngine();
	}

	@Override
	public void tick(BooleanSupplier p_202162_, boolean p_202163_) {}

	@Override
	public int getLoadedChunksCount() {
		return 0;
	}

	public static class EmptierChunk extends LevelChunk {

		private static final class DummyLevel extends Level {

			private final TickRateManager tickManager = new TickRateManager();

			private DummyLevel(WritableLevelData pLevelData, ResourceKey<Level> pDimension,
				RegistryAccess pRegistryAccess, Holder<DimensionType> pDimensionTypeRegistration,
				Supplier<ProfilerFiller> pProfiler, boolean pIsClientSide, boolean pIsDebug, long pBiomeZoomSeed,
				int pMaxChainedNeighborUpdates) {
				super(pLevelData, pDimension, pRegistryAccess, pDimensionTypeRegistration, pProfiler, pIsClientSide, pIsDebug,
					pBiomeZoomSeed, pMaxChainedNeighborUpdates);
				access = pRegistryAccess;
			}

			private final RegistryAccess access;

			private DummyLevel(RegistryAccess access) {
				this(null, null, access, access.registryOrThrow(Registries.DIMENSION_TYPE)
					.getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD), null, false, false, 0, 0);
			}

			@Override
			public ChunkSource getChunkSource() {
				return null;
			}

			@Override
			public void levelEvent(Player pPlayer, int pType, BlockPos pPos, int pData) {}

			@Override
			public void gameEvent(Entity pEntity, GameEvent pEvent, BlockPos pPos) {}

			@Override
			public void gameEvent(GameEvent p_220404_, Vec3 p_220405_, Context p_220406_) {}

			@Override
			public RegistryAccess registryAccess() {
				return access;
			}

			@Override
			public List<? extends Player> players() {
				return null;
			}

			@Override
			public Holder<Biome> getUncachedNoiseBiome(int pX, int pY, int pZ) {
				return null;
			}

			@Override
			public float getShade(Direction pDirection, boolean pShade) {
				return 0;
			}

			@Override
			public void sendBlockUpdated(BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {}

			@Override
			public void playSound(Player pPlayer, double pX, double pY, double pZ, SoundEvent pSound,
				SoundSource pCategory, float pVolume, float pPitch) {}

			@Override
			public void playSound(Player pPlayer, Entity pEntity, SoundEvent pEvent, SoundSource pCategory,
				float pVolume, float pPitch) {}

			@Override
			public void playSeededSound(Player p_220363_, double p_220364_, double p_220365_, double p_220366_,
					SoundEvent p_220367_, SoundSource p_220368_, float p_220369_, float p_220370_, long p_220371_) {}

			@Override
			public void playSeededSound(Player p_220372_, Entity p_220373_, Holder<SoundEvent> p_220374_, SoundSource p_220375_,
					float p_220376_, float p_220377_, long p_220378_) {}

			@Override
			public String gatherChunkSourceStats() {
				return null;
			}

			@Override
			public Entity getEntity(int pId) {
				return null;
			}

			@Override
			public TickRateManager tickRateManager() {
				return tickManager;
			}

			@Override
			public MapItemSavedData getMapData(String pMapName) {
				return null;
			}

			@Override
			public void setMapData(String pMapId, MapItemSavedData pData) {}

			@Override
			public int getFreeMapId() {
				return 0;
			}

			@Override
			public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {}

			@Override
			public Scoreboard getScoreboard() {
				return null;
			}

			@Override
			public RecipeManager getRecipeManager() {
				return null;
			}

			@Override
			protected LevelEntityGetter<Entity> getEntities() {
				return null;
			}

			@Override
			public LevelTickAccess<Block> getBlockTicks() {
				return BlackholeTickAccess.emptyLevelList();
			}

			@Override
			public LevelTickAccess<Fluid> getFluidTicks() {
				return BlackholeTickAccess.emptyLevelList();
			}

			@Override
			public FeatureFlagSet enabledFeatures() {
				return FeatureFlagSet.of();
			}

			@Override
			public void playSeededSound(Player pPlayer, double pX, double pY, double pZ, Holder<SoundEvent> pSound,
				SoundSource pSource, float pVolume, float pPitch, long pSeed) {}
		}

		public EmptierChunk(RegistryAccess registryAccess) {
			super(new DummyLevel(registryAccess), null);
		}

		public BlockState getBlockState(BlockPos p_180495_1_) {
			return Blocks.VOID_AIR.defaultBlockState();
		}

		@Nullable
		public BlockState setBlockState(BlockPos p_177436_1_, BlockState p_177436_2_, boolean p_177436_3_) {
			return null;
		}

		public FluidState getFluidState(BlockPos p_204610_1_) {
			return Fluids.EMPTY.defaultFluidState();
		}

		public int getLightEmission(BlockPos p_217298_1_) {
			return 0;
		}

		@Nullable
		public BlockEntity getBlockEntity(BlockPos p_177424_1_, EntityCreationType p_177424_2_) {
			return null;
		}

		public void addAndRegisterBlockEntity(BlockEntity p_150813_1_) {}

		public void setBlockEntity(BlockEntity p_177426_2_) {}

		public void removeBlockEntity(BlockPos p_177425_1_) {}

		public void markUnsaved() {}

		public boolean isEmpty() {
			return true;
		}

		public boolean isYSpaceEmpty(int p_76606_1_, int p_76606_2_) {
			return true;
		}

		public FullChunkStatus getFullStatus() {
			return FullChunkStatus.FULL;
		}
	}
}
