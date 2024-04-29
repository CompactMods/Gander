package dev.compactmods.gander.render;

import com.mojang.blaze3d.vertex.VertexBuffer;

import dev.compactmods.gander.render.rendertypes.GanderCompositeRenderType;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.Map;
import java.util.stream.Collectors;

public class RenderTypeHelper {
	public static final String MAIN_TARGET = "minecraft:main";

	public static final Map<RenderType, RenderType> REMAPPED_RENDER_TYPES = new Reference2ObjectOpenHashMap<>();

	public static final Map<RenderStateShard.OutputStateShard, String> OUTPUT_STATE_SHARD_MAP = Map.of(
			RenderStateShard.OutputStateShard.MAIN_TARGET, MAIN_TARGET,
			RenderStateShard.OutputStateShard.OUTLINE_TARGET, MAIN_TARGET,
			RenderStateShard.OutputStateShard.TRANSLUCENT_TARGET, "translucent",
			RenderStateShard.OutputStateShard.PARTICLES_TARGET, "particles",
			RenderStateShard.OutputStateShard.WEATHER_TARGET, "weather",
			RenderStateShard.OutputStateShard.CLOUDS_TARGET, "clouds",
			RenderStateShard.OutputStateShard.ITEM_ENTITY_TARGET, "itemEntity"
	);

	public static final Map<RenderType, VertexBuffer> RENDER_TYPE_BUFFERS = RenderType.chunkBufferLayers()
			.stream()
			.collect(Collectors.toMap(renderType -> renderType, renderType -> new VertexBuffer(VertexBuffer.Usage.STATIC)));

	public static RenderType redirectedRenderType(RenderType desiredType, PostChain chain) {
		final var crying = GanderCompositeRenderType.of(desiredType);
		final var remappedQuestionMark = OUTPUT_STATE_SHARD_MAP.get(crying.state().outputState);

		if (remappedQuestionMark == null)
			return desiredType;

		final var AAAAAAAA = REMAPPED_RENDER_TYPES.computeIfAbsent(desiredType, type -> GanderCompositeRenderType.of(type)
				.targetingRenderTarget(chain.getRenderTarget(remappedQuestionMark), chain.getRenderTarget(MAIN_TARGET)));

		return AAAAAAAA;
	}
}
