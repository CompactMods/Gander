package dev.compactmods.gander.render.rendertypes;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.Map;

public record RedirectedRenderTypeStore(PostChain translucencyChain) implements RenderTypeStore {
	public static final String MAIN_TARGET = "minecraft:main";

	private static final Map<RenderType, RenderType> REMAPPED_RENDER_TYPES = new Reference2ObjectOpenHashMap<>();
	private static final Map<RenderStateShard.OutputStateShard, String> OUTPUT_STATE_SHARD_MAP = Map.of(
			RenderStateShard.OutputStateShard.MAIN_TARGET, MAIN_TARGET,
			RenderStateShard.OutputStateShard.OUTLINE_TARGET, MAIN_TARGET,
			RenderStateShard.OutputStateShard.TRANSLUCENT_TARGET, "translucent",
			RenderStateShard.OutputStateShard.PARTICLES_TARGET, "particles",
			RenderStateShard.OutputStateShard.WEATHER_TARGET, "weather",
			RenderStateShard.OutputStateShard.CLOUDS_TARGET, "clouds",
			RenderStateShard.OutputStateShard.ITEM_ENTITY_TARGET, "itemEntity"
	);

	public RenderType redirectedRenderType(RenderType desiredType) {
		final var crying = GanderCompositeRenderType.of(desiredType);
		final var remappedQuestionMark = OUTPUT_STATE_SHARD_MAP.get(crying.state().outputState);

		if (remappedQuestionMark == null)
			return desiredType;

		final var AAAAAAAA = REMAPPED_RENDER_TYPES.computeIfAbsent(desiredType, type -> GanderCompositeRenderType.of(type)
				.targetingRenderTarget(translucencyChain.getRenderTarget(remappedQuestionMark), translucencyChain.getRenderTarget(MAIN_TARGET)));

		return AAAAAAAA;
	}

	public void prepareTranslucency() {
		var target = translucencyChain.getRenderTarget("translucent");
		target.clear(Minecraft.ON_OSX);
		target.copyDepthFrom(translucencyChain.getRenderTarget(RedirectedRenderTypeStore.MAIN_TARGET));
	}

	@Override
	public void processTransclucency(float partialTicks) {
		this.translucencyChain.process(partialTicks);
	}

	@Override
	public void clear() {
		OUTPUT_STATE_SHARD_MAP.values().forEach(output -> {
			final var t = translucencyChain.getRenderTarget(output);
			if (t != null) t.clear(Minecraft.ON_OSX);
		});
	}

	@Override
	public void dispose() {
		REMAPPED_RENDER_TYPES.clear();
		translucencyChain.close();
	}
}
