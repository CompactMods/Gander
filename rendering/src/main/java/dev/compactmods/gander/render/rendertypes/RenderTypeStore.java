package dev.compactmods.gander.render.rendertypes;

import dev.compactmods.gander.render.translucency.TranslucencyChain;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;

public class RenderTypeStore {
	public static final ResourceLocation MAIN_TARGET = ResourceLocation.fromNamespaceAndPath("gander_test", "main");

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
		// TODO: GanderLib.asResource
		final var remappedQuestionMark = ResourceLocation.fromNamespaceAndPath("gander_test", BLOCK_RENDER_TARGET_MAP.get(crying.state().outputState));

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
		// TODO: GanderLib.asResource
		final var remappedQuestionMark = ResourceLocation.fromNamespaceAndPath("gander_test", FLUID_RENDER_TARGET_MAP.get(crying.state().outputState));

		if (remappedQuestionMark == null)
			return desiredType;

		final var AAAAAAAA = REMAPPED_FLUID_RENDER_TYPES.computeIfAbsent(desiredType, type -> GanderCompositeRenderType.of(type)
				.targetingTranslucentRenderTarget(
						translucencyChain.getRenderTarget(remappedQuestionMark),
						translucencyChain.getRenderTarget(MAIN_TARGET)));

		return AAAAAAAA;
	}

	public void processTransclucency(float partialTicks) {
		// TODO: copy this over to TranslucencyChain if necessary
		/*{
			var target = translucencyChain.getRenderTarget("final");
			var other = translucencyChain.getRenderTarget("translucent");
			GlStateManager._glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, other.frameBufferId);
			GlStateManager._glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
			GlStateManager._glBlitFrameBuffer(0,
					0,
					other.width,
					other.height,
					0,
					0,
					target.width,
					target.height,
					GlConst.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT,
					GlConst.GL_NEAREST);
			GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
		}

		{
			var target = translucencyChain.getRenderTarget(RedirectedRenderTypeStore.MAIN_TARGET);
			var other = translucencyChain.getRenderTarget("final");
			GlStateManager._glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, other.frameBufferId);
			GlStateManager._glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
			GlStateManager._glBlitFrameBuffer(
					0, 0, other.width, other.height,
					0, 0, target.width, target.height,
					GlConst.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT,
					GlConst.GL_NEAREST);
			GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
		}*/
	}

	public void dispose() {
		REMAPPED_BLOCK_RENDER_TYPES.clear();
		REMAPPED_FLUID_RENDER_TYPES.clear();
	}
}
