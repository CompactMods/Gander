package dev.compactmods.gander.render.rendertypes;

import java.util.Map;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.render.translucency.TranslucencyChain;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class RenderTypeStore {
	public static final ResourceLocation MAIN_TARGET = ResourceLocation.fromNamespaceAndPath("gander", "main");

	private static final Map<RenderStateShard.OutputStateShard, String> BLOCK_RENDER_TARGET_MAP = Map.of(
			RenderStateShard.OutputStateShard.MAIN_TARGET, "main",
			RenderStateShard.OutputStateShard.OUTLINE_TARGET, "entity",
			RenderStateShard.OutputStateShard.TRANSLUCENT_TARGET, "translucent",
			RenderStateShard.OutputStateShard.PARTICLES_TARGET, "particles",
			RenderStateShard.OutputStateShard.WEATHER_TARGET, "weather",
			RenderStateShard.OutputStateShard.CLOUDS_TARGET, "clouds",
			RenderStateShard.OutputStateShard.ITEM_ENTITY_TARGET, "item_entity"
	);

	private static final Map<RenderStateShard.OutputStateShard, String> FLUID_RENDER_TARGET_MAP = Map.of(
			RenderStateShard.OutputStateShard.MAIN_TARGET, "main",
			RenderStateShard.OutputStateShard.OUTLINE_TARGET, "entity",
			RenderStateShard.OutputStateShard.TRANSLUCENT_TARGET, "water",
			RenderStateShard.OutputStateShard.PARTICLES_TARGET, "particles",
			RenderStateShard.OutputStateShard.WEATHER_TARGET, "weather",
			RenderStateShard.OutputStateShard.CLOUDS_TARGET, "clouds",
			RenderStateShard.OutputStateShard.ITEM_ENTITY_TARGET, "item_entity"
	);

	private final TranslucencyChain translucencyChain;
	private final Map<RenderType, RenderType> REMAPPED_BLOCK_RENDER_TYPES = new Reference2ObjectOpenHashMap<>();
	private final Map<RenderType, RenderType> REMAPPED_FLUID_RENDER_TYPES = new Reference2ObjectOpenHashMap<>();

	public RenderTypeStore(TranslucencyChain translucencyChain) {
		this.translucencyChain = translucencyChain;
	}

	public RenderType redirectedBlockRenderType(RenderType desiredType) {
		final var crying = GanderCompositeRenderType.of(desiredType);
		final var remappedQuestionMark = Gander.asResource(BLOCK_RENDER_TARGET_MAP.get(crying.state().outputState));

		if (remappedQuestionMark == null)
			return desiredType;

		final var AAAAAAAA = REMAPPED_BLOCK_RENDER_TYPES.computeIfAbsent(desiredType, type -> GanderCompositeRenderType.of(type)
				.targetingTranslucentRenderTarget(
						translucencyChain.getRenderTarget(remappedQuestionMark),
						translucencyChain.getRenderTarget(MAIN_TARGET)));

		return AAAAAAAA;
	}

	public RenderType redirectedFluidRenderType(RenderType desiredType) {
		final var crying = GanderCompositeRenderType.of(desiredType);
		final var remappedQuestionMark = Gander.asResource(FLUID_RENDER_TARGET_MAP.get(crying.state().outputState));

		if (remappedQuestionMark == null)
			return desiredType;

		final var AAAAAAAA = REMAPPED_FLUID_RENDER_TYPES.computeIfAbsent(desiredType, type -> GanderCompositeRenderType.of(type)
				.targetingTranslucentRenderTarget(
						translucencyChain.getRenderTarget(remappedQuestionMark),
						translucencyChain.getRenderTarget(MAIN_TARGET)));

		return AAAAAAAA;
	}

	public void dispose() {
		REMAPPED_BLOCK_RENDER_TYPES.clear();
		REMAPPED_FLUID_RENDER_TYPES.clear();
	}
}
