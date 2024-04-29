package dev.compactmods.gander.render.rendertypes;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.Map;

import org.lwjgl.opengl.GL11;

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
		target.bindWrite(true);
		RenderSystem.clearStencil(0);
		RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
		target.clear(Minecraft.ON_OSX);

		var other = translucencyChain.getRenderTarget(RedirectedRenderTypeStore.MAIN_TARGET);
		GlStateManager._glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, other.frameBufferId);
		GlStateManager._glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
		GlStateManager._glBlitFrameBuffer(
				0, 0, other.width, other.height,
				0, 0, target.width, target.height,
				GlConst.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT,
				GlConst.GL_NEAREST);
		GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
	}

	@Override
	public void processTransclucency(float partialTicks) {
		{
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
		}

		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		this.translucencyChain.process(partialTicks);
		RenderSystem.depthMask(false);
		RenderSystem.disableDepthTest();
	}

	@Override
	public void clear() {
		OUTPUT_STATE_SHARD_MAP.values().forEach(output -> {
			final var t = translucencyChain.getRenderTarget(output);
			if (t != null) {
				t.bindWrite(true);
				if (t.isStencilEnabled())
				{
					RenderSystem.clearStencil(0);
					RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
				}
				t.clear(Minecraft.ON_OSX);
			}
		});
	}

	@Override
	public void dispose() {
		REMAPPED_RENDER_TYPES.clear();
		translucencyChain.close();
	}
}
