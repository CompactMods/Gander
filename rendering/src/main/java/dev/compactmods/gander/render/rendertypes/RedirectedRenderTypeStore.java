package dev.compactmods.gander.render.rendertypes;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import cpw.mods.modlauncher.api.ITransformationService.Resource;
import dev.compactmods.gander.render.translucency.TranslucencyChain;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;

import org.lwjgl.opengl.GL11;

public record RedirectedRenderTypeStore(TranslucencyChain translucencyChain) implements RenderTypeStore {
	public static final ResourceLocation MAIN_TARGET = new ResourceLocation("gander", "main");

	private static final Map<RenderType, RenderType> REMAPPED_RENDER_TYPES = new Reference2ObjectOpenHashMap<>();
	private static final Map<RenderStateShard.OutputStateShard, String> OUTPUT_STATE_SHARD_MAP = Map.of(
			RenderStateShard.OutputStateShard.MAIN_TARGET, "main",
			RenderStateShard.OutputStateShard.OUTLINE_TARGET, "entity",
			RenderStateShard.OutputStateShard.TRANSLUCENT_TARGET, "translucent",
			RenderStateShard.OutputStateShard.PARTICLES_TARGET, "particles",
			RenderStateShard.OutputStateShard.WEATHER_TARGET, "weather",
			RenderStateShard.OutputStateShard.CLOUDS_TARGET, "clouds",
			RenderStateShard.OutputStateShard.ITEM_ENTITY_TARGET, "item_entity"
	);

	public RenderType redirectedRenderType(RenderType desiredType) {
		final var crying = GanderCompositeRenderType.of(desiredType);
		// TODO: GanderLib.asResource
		final var remappedQuestionMark = new ResourceLocation("gander", OUTPUT_STATE_SHARD_MAP.get(crying.state().outputState));

		if (remappedQuestionMark == null)
			return desiredType;

		final var AAAAAAAA = REMAPPED_RENDER_TYPES.computeIfAbsent(desiredType, type -> GanderCompositeRenderType.of(type)
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

	@Override
	public void dispose() {
		REMAPPED_RENDER_TYPES.clear();
	}
}
