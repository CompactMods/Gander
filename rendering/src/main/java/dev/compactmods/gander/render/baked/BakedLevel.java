package dev.compactmods.gander.render.baked;

import com.mojang.blaze3d.vertex.BufferBuilder;

import com.mojang.blaze3d.vertex.VertexBuffer;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderType;

import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.stream.Collectors;

public final class BakedLevel {
	private final WeakReference<BlockAndTintGetter> originalLevel;
	private final Map<RenderType, VertexBuffer> renderBuffers;
	private BufferBuilder.@Nullable SortState transparencyState;
	private final BoundingBox blockBoundaries;

	public BakedLevel(WeakReference<BlockAndTintGetter> originalLevel,
					  Map<RenderType, VertexBuffer> renderBuffers,
					  @Nullable BufferBuilder.SortState transparencyState,
					  BoundingBox blockBoundaries) {
		this.originalLevel = originalLevel;
		this.renderBuffers = renderBuffers;
		this.transparencyState = transparencyState;
		this.blockBoundaries = blockBoundaries;
	}

	public void resortTranslucency(Vector3f cameraPosition) {
		this.transparencyState = LevelBakery.sortTranslucency(cameraPosition, this.renderBuffers.keySet());
	}

	public WeakReference<BlockAndTintGetter> originalLevel() {
		return originalLevel;
	}

	public Map<RenderType, VertexBuffer> renderBuffers() {
		return renderBuffers;
	}

	public BufferBuilder.@Nullable SortState transparencyState() {
		return transparencyState;
	}

	public BoundingBox blockBoundaries() {
		return blockBoundaries;
	}
}
