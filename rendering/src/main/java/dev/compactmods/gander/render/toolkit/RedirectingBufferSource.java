package dev.compactmods.gander.render.toolkit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.function.Function;

class RedirectingBufferSource extends MultiBufferSource.BufferSource {

    private final Function<RenderType, RenderType> remapper;

    protected RedirectingBufferSource(Function<RenderType, RenderType> remapper, BufferSource original) {
        super(original.sharedBuffer, original.fixedBuffers);
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
        var bufferbuilder = this.startedBuilders.remove(renderType);
        if (bufferbuilder != null) {
            MeshData meshdata = bufferbuilder.build();
            if (meshdata != null) {
                if (renderType.sortOnUpload()) {
                    ByteBufferBuilder bytebufferbuilder = this.fixedBuffers.getOrDefault(renderType, this.sharedBuffer);
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
