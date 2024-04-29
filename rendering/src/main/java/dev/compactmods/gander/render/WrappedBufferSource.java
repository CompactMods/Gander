package dev.compactmods.gander.render;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.compactmods.gander.render.rendertypes.RedirectedRenderTypeStore;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;

import java.util.Objects;
import java.util.Optional;

class WrappedBufferSource extends MultiBufferSource.BufferSource {

	private final RenderTypeStore renderStore;

	protected WrappedBufferSource(RenderTypeStore renderStore, BufferSource original) {
		super(original.builder, original.fixedBuffers);
		this.renderStore = renderStore;
	}

	public static WrappedBufferSource from(RenderTypeStore renderStore, BufferSource original) {
		return new WrappedBufferSource(renderStore, original);
	}

	@Override
	public void endBatch(RenderType renderType) {
		final var builder = this.fixedBuffers.getOrDefault(renderType, this.builder);

		boolean flag = Objects.equals(this.lastState, renderType.asOptional());
		if (flag || builder != this.builder) {
			if (this.startedBuffers.remove(builder)) {
				renderStore.redirectedRenderType(renderType)
						.end(builder, RenderSystem.getVertexSorting());

				if (flag) {
					this.lastState = Optional.empty();
				}
			}
		}
	}
}
