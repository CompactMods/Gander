package dev.compactmods.gander.level;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jetbrains.annotations.Nullable;

public class VirtualChunkSource extends ChunkSource {
	private final VirtualLevel virtualLevel;
	private final Long2ObjectMap<VirtualChunk> chunks = new Long2ObjectOpenHashMap<>();

	public VirtualChunkSource(VirtualLevel virtualLevel) {
		this.virtualLevel = virtualLevel;
	}

	@Override
	public Level getLevel() {
		return virtualLevel;
	}

	public ChunkAccess getChunk(int x, int z) {
		long pos = ChunkPos.asLong(x, z);
		return chunks.computeIfAbsent(pos, $ -> new VirtualChunk(virtualLevel, x, z));
	}

	@Override
	@Nullable
	public LevelChunk getChunk(int x, int z, boolean load) {
		long pos = ChunkPos.asLong(x, z);
		return chunks.computeIfAbsent(pos, $ -> new VirtualChunk(virtualLevel, x, z));
	}

	@Override
	@Nullable
	public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean load) {
		return getChunk(x, z);
	}

	@Override
	public void tick(BooleanSupplier hasTimeLeft, boolean tickChunks) {
	}

	@Override
	public String gatherStats() {
		return "VirtualChunkSource";
	}

	@Override
	public int getLoadedChunksCount() {
		return chunks.size();
	}

	@Override
	public LevelLightEngine getLightEngine() {
		return virtualLevel.getLightEngine();
	}
}
