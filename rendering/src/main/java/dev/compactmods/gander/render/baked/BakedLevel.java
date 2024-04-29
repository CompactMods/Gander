package dev.compactmods.gander.render.baked;

import com.mojang.blaze3d.vertex.BufferBuilder;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderType;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.stream.Collectors;

public final class BakedLevel {
	private final WeakReference<BlockAndTintGetter> originalLevel;
	private final SectionBufferBuilderPack builders;
	private final Map<RenderType, VertexBuffer> renderBuffers;
	private BufferBuilder.@Nullable SortState transparencyState;
	private final BoundingBox blockBoundaries;

	public BakedLevel(WeakReference<BlockAndTintGetter> originalLevel,
					  SectionBufferBuilderPack builders,
					  Map<RenderType, VertexBuffer> renderBuffers,
					  @Nullable BufferBuilder.SortState transparencyState,
					  BoundingBox blockBoundaries) {
		this.originalLevel = originalLevel;
		this.builders = builders;
		this.renderBuffers = renderBuffers;
		this.transparencyState = transparencyState;
		this.blockBoundaries = blockBoundaries;
	}

	public void resortTranslucency(Vector3f cameraPosition) {
		if (this.transparencyState != null && this.renderBuffers.containsKey(RenderType.translucent())) {
			final var builder = builders.builder(RenderType.translucent());
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
			builder.restoreSortState(this.transparencyState);
			builder.setQuadSorting(VertexSorting.byDistance(cameraPosition.x, cameraPosition.y, cameraPosition.z));
			final var transparencyState = builder.getSortState();
			final var vertexBuffer = renderBuffers.get(RenderType.translucent());
			vertexBuffer.bind();
			vertexBuffer.upload(builder.end());
			VertexBuffer.unbind();
			this.transparencyState = transparencyState;
		}
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
