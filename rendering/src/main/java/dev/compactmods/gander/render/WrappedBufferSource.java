package dev.compactmods.gander.render;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;

import java.util.Objects;
import java.util.Optional;

class WrappedBufferSource extends MultiBufferSource.BufferSource {

	private final PostChain translucencyChain;

	protected WrappedBufferSource(PostChain translucencyChain, BufferSource original) {
		super(original.builder, original.fixedBuffers);
		this.translucencyChain = translucencyChain;
	}

	public static WrappedBufferSource from(PostChain translucencyChain, BufferSource original) {
		return new WrappedBufferSource(translucencyChain, original);
	}

	@Override
	public void endBatch(RenderType renderType) {
		final var builder = this.fixedBuffers.getOrDefault(renderType, this.builder);

		boolean flag = Objects.equals(this.lastState, renderType.asOptional());
		if (flag || builder != this.builder) {
			if (this.startedBuffers.remove(builder)) {
				RenderTypeHelper.redirectedRenderType(renderType, translucencyChain)
						.end(builder, RenderSystem.getVertexSorting());

				if (flag) {
					this.lastState = Optional.empty();
				}
			}
		}
	}
}
