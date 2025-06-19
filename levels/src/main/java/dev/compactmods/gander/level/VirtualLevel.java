package dev.compactmods.gander.level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import dev.compactmods.gander.level.light.VirtualLightEngine;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelDataManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.compactmods.gander.core.math.WorldMath;
//import dev.compactmods.gander.level.block.VirtualBlockSystem;
import dev.compactmods.gander.level.chunk.VirtualChunkSource;
import dev.compactmods.gander.level.entity.VirtualEntitySystem;
import dev.compactmods.gander.level.util.VirtualLevelUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;

public class VirtualLevel extends Level implements WorldGenLevel, TickingLevel {

	private final TickRateManager tickManager = new TickRateManager();
	private final RegistryAccess access;
	private final ChunkSource chunkSource;
    private final VirtualLightEngine lightEngine;
//	private final VirtualBlockSystem blocks;
	private final Scoreboard scoreboard;
	private AABB bounds;
	private VirtualEntitySystem entities;
	private final Holder<Biome> biome;
    private final List<Consumer<VirtualLevel>> onBlockUpdate;
    private final ModelDataManager modelDataManager;
    private final Long2ReferenceMap<BlockEntity> blockEntities;

	protected VirtualLevel(RegistryAccess access, boolean isClientside) {
		this(
				VirtualLevelUtils.LEVEL_DATA, Level.OVERWORLD, access,
				access.holderOrThrow(BuiltinDimensionTypes.OVERWORLD),
				isClientside, false,
				0, 0);
	}

	protected VirtualLevel(WritableLevelData pLevelData, ResourceKey<Level> pDimension,
                         RegistryAccess pRegistryAccess, Holder<DimensionType> pDimensionTypeRegistration,
                         boolean pIsClientSide, boolean pIsDebug, long pBiomeZoomSeed,
                         int pMaxChainedNeighborUpdates) {
		super(pLevelData, pDimension, pRegistryAccess, pDimensionTypeRegistration, pIsClientSide, pIsDebug,
				pBiomeZoomSeed, pMaxChainedNeighborUpdates);
		this.access = pRegistryAccess;
        this.onBlockUpdate = new ArrayList<>();
        this.chunkSource = new VirtualChunkSource(this);
//		this.blocks = new VirtualBlockSystem(this);
        this.lightEngine = new VirtualLightEngine(pos -> 15, skyPos -> 15, () -> this);
		this.scoreboard = new Scoreboard();
		this.bounds = AABB.INFINITE;
		this.entities = new VirtualEntitySystem();
		this.biome = pRegistryAccess.holderOrThrow(Biomes.PLAINS);
        this.modelDataManager = new ModelDataManager(this);
        this.blockEntities = new Long2ReferenceOpenHashMap<>();
	}

    public void addBlockUpdateListener(Consumer<VirtualLevel> listener) {
        this.onBlockUpdate.add(listener);
    }

	public Holder<Biome> getBiome() {
		return biome;
	}

    @Override
    public ModelData getModelData(BlockPos pos) {
        return modelDataManager.getAt(pos);
    }

    @Override
    public @Nullable ModelDataManager getModelDataManager() {
        return modelDataManager;
    }

    @Override
	public PotionBrewing potionBrewing() {
		// Minecraft, why?
		return PotionBrewing.EMPTY;
	}

    @Override
    public FuelValues fuelValues() {
        return null;
    }

    @Override
    public void setDayTimeFraction(float v) {

    }

    @Override
    public float getDayTimeFraction() {
        return 0;
    }

    @Override
    public float getDayTimePerTick() {
        return 0;
    }

    @Override
    public void setDayTimePerTick(float v) {

    }

	@Override
	public ChunkSource getChunkSource() {
		return chunkSource;
	}

    @Override
    public void levelEvent(@Nullable Entity entity, int i, BlockPos blockPos, int i1) {

    }

//    public VirtualBlockSystem blockSystem() {
//		return this.blocks;
//	}

//	@Override
//	public boolean setBlock(BlockPos pPos, BlockState pNewState, int pFlags) {
//		return this.setBlock(pPos, pNewState, 0, 512);
//	}
//
//	@Override
//	public boolean setBlock(BlockPos pos, BlockState state, int pFlags, int pRecursionLeft) {
//		if (this.isOutsideBuildHeight(pos)) {
//			return false;
//		}
//
//		return blocks.blockAndFluidStorage().setBlock(pos, state, pFlags, pRecursionLeft);
//	}
//
//	@Override
//	public boolean setBlockAndUpdate(BlockPos pPos, BlockState pState) {
//		return this.setBlock(pPos, pState, Block.UPDATE_NONE);
//	}
//
	@Override
	public void setBlockEntity(BlockEntity blockEntity) {
		super.setBlockEntity(blockEntity);
        this.blockEntities.put(blockEntity.getBlockPos().asLong(), blockEntity);
	}
//
	@Override
	public void removeBlockEntity(final BlockPos pPos) {
		super.removeBlockEntity(pPos);
        this.untrackBlockEntity(pPos);
	}
//
//	@Nullable
//	@Override
//	public BlockEntity getBlockEntity(BlockPos pPos) {
//		if (!bounds.contains(Vec3.atCenterOf(pPos)))
//			return null;
//
//		return blocks.blockAndFluidStorage().getBlockEntity(pPos);
//	}
//
//	@Override
//	public BlockState getBlockState(BlockPos pPos) {
//		if (!bounds.contains(Vec3.atCenterOf(pPos)))
//			return Blocks.AIR.defaultBlockState();
//
//		return blocks.blockAndFluidStorage().getBlockState(pPos);
//	}
//
//	@Override
//	public @NotNull FluidState getFluidState(BlockPos pos) {
//		if (!bounds.contains(Vec3.atCenterOf(pos)))
//			return Fluids.EMPTY.defaultFluidState();
//
//		return blocks.blockAndFluidStorage().getFluidState(pos);
//	}

    @Override
    public void playSeededSound(@Nullable Entity entity, double v, double v1, double v2, Holder<SoundEvent> holder, SoundSource soundSource, float v3, float v4, long l) {

    }

    @Override
    public void playSeededSound(@Nullable Entity entity, Entity entity1, Holder<SoundEvent> holder, SoundSource soundSource, float v, float v1, long l) {

    }

    public void animateTick() {
		animateBlockTick(WorldMath.randomPosInAABB(random, bounds));
	}

	@Override
	public void tick(final float deltaTime) {
		tickBlockEntities();
	}

	protected void animateBlockTick(BlockPos pBlockPos) {
		BlockState blockstate = this.getBlockState(pBlockPos);
		blockstate.getBlock().animateTick(blockstate, this, pBlockPos, random);
		FluidState fluidstate = this.getFluidState(pBlockPos);
		if (!fluidstate.isEmpty()) {
			fluidstate.animateTick(this, pBlockPos, random);
		}

		if (!blockstate.isCollisionShapeFullBlock(this, pBlockPos)) {
			this.getBiome(pBlockPos)
					.value()
					.getAmbientParticle()
					.filter(aps -> aps.canSpawn(random))
					.ifPresent((p_264703_) -> {
						this.addParticle(p_264703_.getOptions(), (double) pBlockPos.getX() + this.random.nextDouble(), (double) pBlockPos.getY() + this.random.nextDouble(), (double) pBlockPos.getZ() + this.random.nextDouble(), 0.0D, 0.0D, 0.0D);
					});
		}
	}

//	@Override
//	protected void tickBlockEntities() {
//		if (tickRateManager().runsNormally()) {
//			blocks.blockAndFluidStorage().blockEntityPositions()
//					.filter(this::shouldTickBlocksAt)
//					.forEach(entityPos -> {
//						var blockEntity = blocks.blockAndFluidStorage().getBlockEntity(entityPos);
//						if (blockEntity != null) {
//							var ticker = blocks.blockAndFluidStorage().getBlockState(entityPos).getTicker(this, blockEntity.getType());
//
//							if (ticker != null)
//								tickBlockEntity(blockEntity, (BlockEntityTicker<BlockEntity>) ticker);
//						}
//					});
//		}
//	}

    @Override
    public void explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator explosionDamageCalculator, double v, double v1, double v2, float v3, boolean b, ExplosionInteraction explosionInteraction, ParticleOptions particleOptions, ParticleOptions particleOptions1, Holder<SoundEvent> holder) {

    }

    private <T extends BlockEntity> void tickBlockEntity(T blockEntity, BlockEntityTicker<T> ticker) {
		ticker.tick(this, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity);
	}

    @Override
	public void gameEvent(Entity pEntity, Holder<GameEvent> pEvent, BlockPos pPos) {
	}

	@Override
	public void gameEvent(Holder<GameEvent> evt, Vec3 origin, GameEvent.Context ctx) {
	}

	@Override
	public RegistryAccess registryAccess() {
		return access;
	}

	@Override
	public List<? extends Player> players() {
		return List.of();
	}

	@Override
	public Holder<Biome> getUncachedNoiseBiome(int pX, int pY, int pZ) {
		return biome;
	}

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
	public float getShade(Direction pDirection, boolean pShade) {
		return 1f;
	}

	@Override
	public void sendBlockUpdated(BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {
        this.onBlockUpdate.forEach(c -> c.accept(this));
	}

	@Override
	public String gatherChunkSourceStats() {
		return null;
	}

	@Override
	public Entity getEntity(int pId) {
		return entities.getEntity(pId);
	}

    @Override
    public Collection<PartEntity<?>> dragonParts() {
        return List.of();
    }

    @Override
	public TickRateManager tickRateManager() {
		return tickManager;
	}

	@Override
	public MapItemSavedData getMapData(MapId pMapName) {
		return null;
	}

	@Override
	public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {
	}

	@Override
	public Scoreboard getScoreboard() {
		return scoreboard;
	}

    @Override
    public RecipeAccess recipeAccess() {
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
		return FeatureFlags.REGISTRY.allFlags();
	}

	@Override
	public ServerLevel getLevel() {
		// TODO - Virtual Server Level implementation?
		return null;
	}

	public void setBounds(AABB bounds) {
		this.bounds = bounds;
	}

	@Override
	public @NotNull LevelLightEngine getLightEngine() {
		return this.lightEngine;
	}

	@Override
	public int getBrightness(LightLayer pLightType, BlockPos pBlockPos) {
		return 15;
	}

	@Override
	public long getSeed() {
		return 0;
	}

    public AABB getBounds() {
        return bounds;
    }

    public void refreshBlockEntityModels() {
        this.blockEntities.values()
            .forEach(modelDataManager::requestRefresh);
    }

    public Stream<BlockPos> blockEntityPositions() {
        return blockEntities.keySet().longStream()
            .mapToObj(BlockPos::of)
            .map(BlockPos::immutable);
    }

    public void untrackBlockEntity(BlockPos pos) {
        this.blockEntities.remove(pos.asLong());
    }
}
