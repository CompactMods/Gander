package dev.compactmods.gander.render.toolkit;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

class RedirectingBufferSource extends MultiBufferSource.BufferSource {

	private final Function<RenderType, RenderType> remapper;

	protected RedirectingBufferSource(Function<RenderType, RenderType> remapper, BufferSource original) {
		super(original.builder, original.fixedBuffers);
		this.remapper = remapper;
	}

	public static RedirectingBufferSource forBlocks(RenderTypeStore renderStore, BufferSource original) {
		return new RedirectingBufferSource(renderStore::redirectedBlockRenderType, original);
	}

	public static RedirectingBufferSource forFluids(RenderTypeStore renderStore, BufferSource original) {
		return new RedirectingBufferSource(renderStore::redirectedFluidRenderType, original);
	}

	@Override
	public void endBatch(RenderType renderType) {
		final var builder = this.fixedBuffers.getOrDefault(renderType, this.builder);

		boolean flag = Objects.equals(this.lastState, renderType.asOptional());
		if (flag || builder != this.builder) {
			if (this.startedBuffers.remove(builder)) {
				remapper.apply(renderType)
					.end(builder, RenderSystem.getVertexSorting());

				if (flag) {
					this.lastState = Optional.empty();
				}
			}
		}
	}
}