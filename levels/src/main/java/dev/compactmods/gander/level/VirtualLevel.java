package dev.compactmods.gander.level;

import dev.compactmods.gander.level.gen.VirtualChunkSource;
import dev.compactmods.gander.level.light.VirtualLightEngine;
import dev.compactmods.gander.level.util.VirtualLevelUtils;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class VirtualLevel extends Level implements ServerLevelAccessor, WorldGenLevel, TickingLevel {

	private final TickRateManager tickManager = new TickRateManager();
	private final RegistryAccess access;
	private final VirtualChunkSource chunkSource;
	private final LevelLightEngine lightEngine;
	private final VirtualLevelBlocks blocks;
	private final Scoreboard scoreboard;
	// private final StructureManager structureManager;
	private BoundingBox bounds;
	private final Holder<Biome> biome;

	private VirtualLevel(WritableLevelData pLevelData, ResourceKey<Level> pDimension,
						 RegistryAccess pRegistryAccess, Holder<DimensionType> pDimensionTypeRegistration,
						 Supplier<ProfilerFiller> pProfiler, boolean pIsClientSide, boolean pIsDebug, long pBiomeZoomSeed,
						 int pMaxChainedNeighborUpdates) {
		super(pLevelData, pDimension, pRegistryAccess, pDimensionTypeRegistration, pProfiler, pIsClientSide, pIsDebug,
				pBiomeZoomSeed, pMaxChainedNeighborUpdates);
		this.access = pRegistryAccess;
		this.chunkSource = new VirtualChunkSource(this);
		this.lightEngine = new VirtualLightEngine(block -> 15, sky -> 15, this);
		this.blocks = new VirtualLevelBlocks(this::getBiome);
		this.scoreboard = new Scoreboard();
		this.bounds = BoundingBox.fromCorners(BlockPos.ZERO, BlockPos.ZERO);
		// this.structureManager = new StructureManager(this, WorldOptions.DEMO_OPTIONS, StructureCheck);

		biome = pRegistryAccess.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
	}

	public VirtualLevel(RegistryAccess access) {
		this(
				VirtualLevelUtils.LEVEL_DATA, Level.OVERWORLD, access,
				access.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
				VirtualLevelUtils.PROFILER, true, false,
				0, 0);
	}

	public Holder<Biome> getBiome() {
		return biome;
	}

	@Override
	public PotionBrewing potionBrewing()
	{
		// Minecraft, why?
		return PotionBrewing.EMPTY;
	}

	@Override
	public MapId getFreeMapId()
	{
		return new MapId(0);
	}

	@Override
	public ChunkSource getChunkSource() {
		return chunkSource;
	}

	VirtualLevelBlocks blocks() {
		return this.blocks;
	}

	@Override
	public boolean setBlock(BlockPos pPos, BlockState pNewState, int pFlags) {
		return this.setBlock(pPos, pNewState, 0, 512);
	}

	@Override
	public boolean setBlock(BlockPos pos, BlockState state, int pFlags, int pRecursionLeft) {
		if (this.isOutsideBuildHeight(pos)) {
			return false;
		} else {
			blocks.setBlockState(pos, state);
			if(state.hasBlockEntity()) {
				var be = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
				if(be != null) {
					blocks.setBlockEntity(pos, be);
					setBlockEntity(be);
					be.setLevel(this);
				}
			}

			return true;
		}
	}

	@Override
	public boolean setBlockAndUpdate(BlockPos pPos, BlockState pState) {
		return this.setBlock(pPos, pState, Block.UPDATE_NONE);
	}

	@Override
	public void setBlockEntity(BlockEntity blockEntity) {
		BlockPos blockpos = blockEntity.getBlockPos();
		if (this.getBlockState(blockpos).hasBlockEntity()) {
			blockEntity.setLevel(this);
			blockEntity.clearRemoved();
			BlockEntity blockentity = blocks.setBlockEntity(blockpos.immutable(), blockEntity);
			if (blockentity != null && blockentity != blockEntity) {
				blockentity.setRemoved();
			}
		}
	}

	@Override
	public void removeBlockEntity(final BlockPos pPos)
	{
		this.blocks().removeBlockEntity(pPos);
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pPos) {
		if(!bounds.isInside(pPos))
			return null;

		return blocks.getBlockEntity(pPos);
	}

	@Override
	public BlockState getBlockState(BlockPos pPos) {
		if (!bounds.isInside(pPos))
			return Blocks.AIR.defaultBlockState();

		return blocks.getBlockState(pPos);
	}

	@Override
	public @NotNull FluidState getFluidState(BlockPos pos) {
		if(!bounds.isInside(pos))
			return Fluids.EMPTY.defaultFluidState();

		return blocks.getFluidState(pos);
	}

	public void animateTick() {
		animateBlockTick(new BlockPos(
			random.nextIntBetweenInclusive(bounds.minX(), bounds.maxX()),
			random.nextIntBetweenInclusive(bounds.minY(), bounds.maxY()),
			random.nextIntBetweenInclusive(bounds.minZ(), bounds.maxZ())));
	}

	@Override
	public void tick(final float deltaTime)
	{
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

	@Override
	protected void tickBlockEntities()
	{
		if (tickRateManager().runsNormally())
		{
			blocks().getBlockEntities()
				.filter(be -> shouldTickBlocksAt(be.getBlockPos()))
				.forEach(entity -> {
					var ticker = entity.getBlockState().getTicker(this, entity.getType());

					if (ticker != null)
						tickBlockEntity(entity, (BlockEntityTicker<BlockEntity>)ticker);
				});
		}
	}

	private <T extends BlockEntity> void tickBlockEntity(T blockEntity, BlockEntityTicker<T> ticker)
	{
		ticker.tick(this, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity);
	}

	@Override
	public void levelEvent(Player pPlayer, int pType, BlockPos pPos, int pData) {
	}

	@Override
	public void gameEvent(Entity pEntity, Holder<GameEvent> pEvent, BlockPos pPos) {
	}

	@Override
	public void gameEvent(Holder<GameEvent> p_220404_, Vec3 p_220405_, GameEvent.Context p_220406_) {
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
	public float getShade(Direction pDirection, boolean pShade) {
		return 1f;
	}

	@Override
	public void sendBlockUpdated(BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {
	}

	@Override
	public void playSound(Player pPlayer, double pX, double pY, double pZ, SoundEvent pSound,
						  SoundSource pCategory, float pVolume, float pPitch) {
	}

	@Override
	public void playSound(Player pPlayer, Entity pEntity, SoundEvent pEvent, SoundSource pCategory,
						  float pVolume, float pPitch) {
	}

	@Override
	public void playSeededSound(Player p_220363_, double p_220364_, double p_220365_, double p_220366_,
								SoundEvent p_220367_, SoundSource p_220368_, float p_220369_, float p_220370_, long p_220371_) {
	}

	@Override
	public void playSeededSound(Player p_220372_, Entity p_220373_, Holder<SoundEvent> p_220374_, SoundSource p_220375_,
								float p_220376_, float p_220377_, long p_220378_) {
	}

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
	public MapItemSavedData getMapData(MapId pMapName) {
		return null;
	}

	@Override
	public void setMapData(MapId pMapId, MapItemSavedData pData) {
	}

	@Override
	public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {
	}

	@Override
	public Scoreboard getScoreboard() {
		return scoreboard;
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
		return FeatureFlags.DEFAULT_FLAGS;
	}

	@Override
	public void playSeededSound(Player pPlayer, double pX, double pY, double pZ, Holder<SoundEvent> pSound,
								SoundSource pSource, float pVolume, float pPitch, long pSeed) {
	}

	@Override
	public ServerLevel getLevel() {
		return null;
	}

	public void setBounds(BoundingBox bounds) {
		this.bounds = bounds;
	}

	@Override
	public @NotNull LevelLightEngine getLightEngine() {
		return lightEngine;
	}

	@Override
	public int getBrightness(LightLayer pLightType, BlockPos pBlockPos) {
		return 15;
	}

	@Override
	public long getSeed() {
		return 0;
	}
}
