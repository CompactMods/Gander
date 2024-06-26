package dev.compactmods.gander.render;

import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

class WrappedBufferSource extends MultiBufferSource.BufferSource {

	private final Function<RenderType, RenderType> remapper;

	protected WrappedBufferSource(Function<RenderType, RenderType> remapper, BufferSource original) {
		super(original.sharedBuffer, original.fixedBuffers);
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
		var bufferbuilder = this.startedBuilders.remove(renderType);
		if (bufferbuilder != null) {
			MeshData meshdata = bufferbuilder.build();
			if (meshdata != null) {
				if (renderType.sortOnUpload()) {
					ByteBufferBuilder bytebufferbuilder = (ByteBufferBuilder)this.fixedBuffers.getOrDefault(renderType, this.sharedBuffer);
					meshdata.sortQuads(bytebufferbuilder, RenderSystem.getVertexSorting());
				}

				renderType.draw(meshdata);
			}

			if (renderType.equals(this.lastSharedType)) {
				this.lastSharedType = null;
			}
		}
	}
}
