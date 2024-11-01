package dev.compactmods.gander.render.geometry;

import com.mojang.blaze3d.vertex.BufferBuilder;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.renderer.RenderType;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.Map;

public final class BakedLevel {
	private final WeakReference<BlockAndTintGetter> originalLevel;
	private final SectionBufferBuilderPack blockBuilders;
	private final SectionBufferBuilderPack fluidBuilders;
	private final Map<RenderType, VertexBuffer> blockBuffers;
	private final Map<RenderType, VertexBuffer> fluidBuffers;
	private MeshData.SortState blockTransparencyState;
	private MeshData.SortState fluidTransparencyState;
	private final BoundingBox blockBoundaries;

	public BakedLevel(WeakReference<BlockAndTintGetter> originalLevel,
					  SectionBufferBuilderPack blockBuilders,
					  SectionBufferBuilderPack fluidBuilders,
					  Map<RenderType, VertexBuffer> blockBuffers,
					  Map<RenderType, VertexBuffer> fluidBuffers,
					  @Nullable MeshData.SortState blockTransparencyState,
					  @Nullable MeshData.SortState fluidTransparencyState,
					  BoundingBox blockBoundaries) {
		this.originalLevel = originalLevel;
		this.blockBuilders = blockBuilders;
		this.fluidBuilders = fluidBuilders;
		this.blockBuffers = blockBuffers;
		this.fluidBuffers = fluidBuffers;
		this.blockTransparencyState = blockTransparencyState;
		this.fluidTransparencyState = fluidTransparencyState;
		this.blockBoundaries = blockBoundaries;
	}

	public void resortTranslucency(Vector3f cameraPosition) {
		this.blockTransparencyState = resortTranslucency(cameraPosition, blockBuilders, blockBuffers, blockTransparencyState);
		this.fluidTransparencyState = resortTranslucency(cameraPosition, fluidBuilders, fluidBuffers, fluidTransparencyState);
	}

	private MeshData.SortState resortTranslucency(Vector3f cameraPosition, SectionBufferBuilderPack pack, Map<RenderType, VertexBuffer> blockBuffers, MeshData.SortState transparencyState) {
		if (transparencyState != null && blockBuffers.containsKey(RenderType.translucent())) {
			final var builder = pack.buffer(RenderType.translucent());
//			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
//			builder.restoreSortState(transparencyState);
//			builder.setQuadSorting(VertexSorting.byDistance(cameraPosition.x, cameraPosition.y, cameraPosition.z));
//			final var newTransparencyState = builder.getSortState();
//			final var vertexBuffer = blockBuffers.get(RenderType.translucent());
//			vertexBuffer.bind();
//			vertexBuffer.upload(builder.end());
//			VertexBuffer.unbind();
//			return newTransparencyState;
            return transparencyState;
		}

		return null;
	}

	public WeakReference<BlockAndTintGetter> originalLevel() {
		return originalLevel;
	}

	public Map<RenderType, VertexBuffer> blockRenderBuffers() {
		return blockBuffers;
	}

	public Map<RenderType, VertexBuffer> fluidRenderBuffers() {
		return fluidBuffers;
	}

	public @Nullable MeshData.SortState blockTransparencyState() {
		return blockTransparencyState;
	}

	public @Nullable MeshData.SortState fluidTransparencyState() {
		return fluidTransparencyState;
	}

	public BoundingBox blockBoundaries() {
		return blockBoundaries;
	}
}
