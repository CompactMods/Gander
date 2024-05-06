package dev.compactmods.gander.level;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import static net.minecraft.world.level.chunk.ProtoChunk.packOffsetCoordinates;

import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.TickContainerAccess;

public class VirtualChunk extends EmptyLevelChunk {

	private final VirtualLevel virtualLevel;
	private final VirtualChunkSection[] sections;

	private boolean needsLight;

	public VirtualChunk(VirtualLevel world, int x, int z) {
		super(world, new ChunkPos(x, z), VirtualLevelUtils.PLAINS.get());

		this.virtualLevel = world;

		int sectionCount = world.getSectionsCount();
		this.sections = new VirtualChunkSection[sectionCount];

		for (int i = 0; i < sectionCount; i++) {
			sections[i] = new VirtualChunkSection(this, i << 4);
		}

		this.needsLight = true;
	}

	@Override
	@Nullable
	public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
		return virtualLevel.blocks().setBlockState(pos, state);
	}

	@Override
	public void markPosForPostprocessing(BlockPos pPos) {
		if (!this.isOutsideBuildHeight(pPos)) {
			ChunkAccess.getOrCreateOffsetList(this.postProcessing, this.getSectionIndex(pPos.getY())).add(packOffsetCoordinates(pPos));
		}
	}

	@Override
	public void setBlockEntity(BlockEntity blockEntity) {
		virtualLevel.blocks().setBlockEntity(blockEntity.getBlockPos(), blockEntity);
	}

	@Override
	public void addEntity(Entity entity) {
	}

	@Override
	public Set<BlockPos> getBlockEntitiesPos() {
		return Collections.emptySet();
	}

	@Override
	public LevelChunkSection[] getSections() {
		return sections;
	}

	@Override
	public Collection<Map.Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
		return Collections.emptySet();
	}

	@Override
	public void setHeightmap(Heightmap.Types type, long[] data) {
	}

	@Override
	public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
		return null;
	}

	@Override
	public int getHeight(Heightmap.Types type, int x, int z) {
		return 0;
	}

	@Override
	@Nullable
	public StructureStart getStartForStructure(Structure structure) {
		return null;
	}

	@Override
	public void setStartForStructure(Structure structure, StructureStart structureStart) {
	}

	@Override
	public Map<Structure, StructureStart> getAllStarts() {
		return Collections.emptyMap();
	}

	@Override
	public void setAllStarts(Map<Structure, StructureStart> structureStarts) {
	}

	@Override
	public LongSet getReferencesForStructure(Structure pStructure) {
		return LongSets.emptySet();
	}

	@Override
	public void addReferenceForStructure(Structure structure, long reference) {
	}

	@Override
	public Map<Structure, LongSet> getAllReferences() {
		return Collections.emptyMap();
	}

	@Override
	public void setAllReferences(Map<Structure, LongSet> structureReferencesMap) {
	}

	@Override
	public void setUnsaved(boolean unsaved) {
	}

	@Override
	public boolean isUnsaved() {
		return false;
	}

	@Override
	public ChunkStatus getStatus() {
		return ChunkStatus.LIGHT;
	}

	@Override
	public void removeBlockEntity(BlockPos pos) {
		virtualLevel.blocks().removeBlockEntity(pos);
	}

	@Override
	public ShortList[] getPostProcessing() {
		return postProcessing;
	}

	@Override
	@Nullable
	public CompoundTag getBlockEntityNbt(BlockPos pos) {
		return null;
	}

	@Override
	@Nullable
	public CompoundTag getBlockEntityNbtForSaving(BlockPos pos, final HolderLookup.Provider registries) {
		return null;
	}

	@Override
	public void findBlocks(BiPredicate<BlockState, BlockPos> predicate, BiConsumer<BlockPos, BlockState> consumer) {
		virtualLevel.blocks().findBlocks(predicate, consumer);
	}

	@Override
	public TickContainerAccess<Block> getBlockTicks() {
		return BlackholeTickAccess.emptyContainer();
	}

	@Override
	public TickContainerAccess<Fluid> getFluidTicks() {
		return BlackholeTickAccess.emptyContainer();
	}

	@Override
	public TicksToSave getTicksForSerialization() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getInhabitedTime() {
		return 0;
	}

	@Override
	public void setInhabitedTime(long amount) {
	}

	@Override
	public boolean isLightCorrect() {
		return needsLight;
	}

	@Override
	public void setLightCorrect(boolean lightCorrect) {
		this.needsLight = lightCorrect;
	}

	@Override
	@Nullable
	public BlockEntity getBlockEntity(BlockPos pos) {
		return virtualLevel.blocks().getBlockEntity(pos);
	}

	@org.jetbrains.annotations.Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pos, EntityCreationType pCreationType) {
		return virtualLevel.blocks().getBlockEntity(pos);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return virtualLevel.blocks().getBlockState(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return virtualLevel.blocks().getFluidState(pos);
	}
}
