package dev.compactmods.gander.render.geometry;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.MeshData;

import net.minecraft.client.renderer.RenderType;

import java.util.Map;

public record SortedGeometryBufferResult(Map<RenderType, GpuBuffer> buffers, Map<RenderType, MeshData.SortState> meshStates) {
}
