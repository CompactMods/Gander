package dev.compactmods.gander.render.geometry;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;

import net.minecraft.client.renderer.RenderType;

import java.util.Map;

public record SortedGeometryBufferResult(Map<RenderType, VertexBuffer> buffers, Map<RenderType, MeshData.SortState> meshStates) {
}
