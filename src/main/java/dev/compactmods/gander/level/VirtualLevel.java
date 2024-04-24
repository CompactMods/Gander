package dev.compactmods.gander.level;

import dev.compactmods.gander.things.BoundedBlockAndTintGetter;
import dev.compactmods.gander.utility.BBHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class VirtualLevel extends Level implements ServerLevelAccessor, BoundedBlockAndTintGetter {


	private final TickRateManager tickManager = new TickRateManager();
	private final RegistryAccess access;
	private final VirtualChunkSource chunkSource;
	private final LevelLightEngine lightEngine;
	private BoundingBox bounds;

	private VirtualLevel(WritableLevelData pLevelData, ResourceKey<Level> pDimension,
						 RegistryAccess pRegistryAccess, Holder<DimensionType> pDimensionTypeRegistration,
						 Supplier<ProfilerFiller> pProfiler, boolean pIsClientSide, boolean pIsDebug, long pBiomeZoomSeed,
						 int pMaxChainedNeighborUpdates) {
		super(pLevelData, pDimension, pRegistryAccess, pDimensionTypeRegistration, pProfiler, pIsClientSide, pIsDebug,
				pBiomeZoomSeed, pMaxChainedNeighborUpdates);
		this.access = pRegistryAccess;
		this.chunkSource = new VirtualChunkSource(this);
		this.lightEngine = new VirtualLightEngine(block -> 15, sky -> 15, this);
		this.bounds = BoundingBox.fromCorners(BlockPos.ZERO, BlockPos.ZERO);
	}

	public VirtualLevel(RegistryAccess access) {
		this(VirtualLevelUtils.LEVEL_DATA, Level.OVERWORLD, access,
				access.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
				null, true, false,
				0, 0);
	}

	@Override
	public ChunkSource getChunkSource() {
		return chunkSource;
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
			LevelChunk chunk = this.getChunkAt(pos);
			pos = pos.immutable();
			chunk.setBlockState(pos, state, (pFlags & 64) != 0);
			return true;
		}
	}

	@Override
	public boolean setBlockAndUpdate(BlockPos pPos, BlockState pState) {
		return this.setBlock(pPos, pState, Block.UPDATE_NONE);
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pPos) {
		return super.getBlockEntity(pPos);
	}

	public void animateTick() {
		// TODO
//		blocks.keySet()
//				.stream()
//				.filter(p -> random.nextIntBetweenInclusive(1, 10) <= 3)
//				.forEach(this::animateBlockTick);
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
	public void levelEvent(Player pPlayer, int pType, BlockPos pPos, int pData) {
	}

	@Override
	public void gameEvent(Entity pEntity, GameEvent pEvent, BlockPos pPos) {
	}

	@Override
	public void gameEvent(GameEvent p_220404_, Vec3 p_220405_, GameEvent.Context p_220406_) {
	}

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
	public MapItemSavedData getMapData(String pMapName) {
		return null;
	}

	@Override
	public void setMapData(String pMapId, MapItemSavedData pData) {
	}

	@Override
	public int getFreeMapId() {
		return 0;
	}

	@Override
	public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {
	}

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
								SoundSource pSource, float pVolume, float pPitch, long pSeed) {
	}

	@Override
	public ServerLevel getLevel() {
		return null;
	}

	@Override
	public Stream<BlockEntity> blockEntities() {
		return BBHelper.chunksInBounds(bounds)
				.map(p -> this.getChunk(p.x, p.z))
				.flatMap(chunk -> chunk.getBlockEntities().values().stream());
	}

	public void setBounds(BoundingBox bounds) {
		this.bounds = bounds;
	}

	@Override
	public BoundingBox bounds() {
		return bounds;
	}

	@Override
	public @NotNull LevelLightEngine getLightEngine() {
		return lightEngine;
	}

	@Override
	public int getBrightness(LightLayer pLightType, BlockPos pBlockPos) {
		return 15;
	}
}
