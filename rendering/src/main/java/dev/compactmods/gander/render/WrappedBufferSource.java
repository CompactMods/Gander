package dev.compactmods.gander.render;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

class WrappedBufferSource extends MultiBufferSource.BufferSource {

	private final Function<RenderType, RenderType> remapper;

	protected WrappedBufferSource(Function<RenderType, RenderType> remapper, BufferSource original) {
		super(original.builder, original.fixedBuffers);
		this.remapper = remapper;
	}

	public static WrappedBufferSource forBlocks(RenderTypeStore renderStore, BufferSource original) {
		return new WrappedBufferSource(renderStore::redirectedBlockRenderType, original);
	}

	public static WrappedBufferSource forFluids(RenderTypeStore renderStore, BufferSource original) {
		return new WrappedBufferSource(renderStore::redirectedFluidRenderType, original);
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
