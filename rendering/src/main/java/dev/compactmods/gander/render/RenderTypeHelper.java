package dev.compactmods.gander.render;

import com.mojang.blaze3d.vertex.VertexBuffer;

import net.minecraft.client.renderer.RenderType;

import java.util.Map;
import java.util.stream.Collectors;

public class RenderTypeHelper {
	public static final Map<RenderType, VertexBuffer> RENDER_TYPE_BUFFERS = RenderType.chunkBufferLayers()
			.stream()
			.collect(Collectors.toMap(renderType -> renderType, renderType -> new VertexBuffer(VertexBuffer.Usage.STATIC)));

}
