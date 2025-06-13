package dev.compactmods.gander.render.geometry;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;

import com.mojang.blaze3d.vertex.MeshData.DrawState;
import com.mojang.blaze3d.vertex.VertexFormat;

import com.mojang.realmsclient.gui.screens.UploadResult;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntSortedMap.FastSortedEntrySet;
import net.minecraft.client.renderer.RenderType;

import net.minecraft.core.SectionPos;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class CombinedRenderTypeBufferPool {
    public static final int NUMBER_OF_SUB_BUFFERS = 1024;
    public static final int SUB_ALLOCATION_SIZE = 1 << 16;
    private final Long2IntLinkedOpenHashMap sectionOffsets = new Long2IntLinkedOpenHashMap();

    private final GpuBuffer indexPool = RenderSystem.getDevice()
        .createBuffer(() -> "Gander index buffer pool", BufferType.INDICES, BufferUsage.DYNAMIC_WRITE,
            NUMBER_OF_SUB_BUFFERS * SUB_ALLOCATION_SIZE);
    private final GpuBuffer vertexPool = RenderSystem.getDevice()
        .createBuffer(() -> "Gander vertex buffer pool", BufferType.VERTICES, BufferUsage.DYNAMIC_WRITE,
            NUMBER_OF_SUB_BUFFERS * SUB_ALLOCATION_SIZE);

    public CombinedRenderTypeBufferPool() {
        sectionOffsets.defaultReturnValue(-1);
    }

    public GpuBuffer getVertexBuffer() {
        return vertexPool;
    }

    public GpuBuffer getIndexBuffer() {
        return indexPool;
    }

    public int getOffsetOf(SectionPos pos) {
        return sectionOffsets.getAndMoveToFirst(pos.asLong());
    }

    private int runningOffset = 0;
    public Function<CommandEncoder, UploadResult> queueUpload(SectionPos pos, MeshData mesh) {
        int offset = -1;
        if (sectionOffsets.size() >= NUMBER_OF_SUB_BUFFERS) {
            // Pool is full, recycle the last sub allocation
            offset = sectionOffsets.removeLastInt();
            sectionOffsets.putAndMoveToFirst(pos.asLong(), offset);
        }
        else  {
            // Pool still has space, return a new sub allocation
            offset = sectionOffsets.putAndMoveToFirst(pos.asLong(), runningOffset);
            runningOffset += SUB_ALLOCATION_SIZE;
        }

        if (offset < 0)  throw new IllegalStateException("We somehow failed to create a sub-allocation");

        final int finalOffset = offset;
        return encoder -> {
            encoder.writeToBuffer(vertexPool, mesh.vertexBuffer(), finalOffset);

            var indexBuffer = mesh.indexBuffer();
            if (indexBuffer != null) {
                // FIXME: This is some ugly code to emulate baseVertex as we don't have direct access to glDrawElementsBaseVertex
                try (var builder = new ByteBufferBuilder(indexBuffer.limit())) {
                    var ptr = builder.reserve(indexBuffer.limit());

                    // This is, in effect, our baseVertex that we'd pass to glDrawElementsBaseVertex
                    var vertexOffset = finalOffset / mesh.drawState().format().getVertexSize();

                    switch (mesh.drawState().indexType()) {
                        case SHORT -> {
                            var shorts = indexBuffer.asShortBuffer();
                            for (int i = 0; i < shorts.limit(); i++) {
                                MemoryUtil.memPutShort(ptr + i * 2L, (short)(vertexOffset + shorts.get(i)));
                            }
                        }
                        case INT -> {
                            var ints = indexBuffer.asIntBuffer();
                            for (int i = 0; i < ints.limit(); i++) {
                                MemoryUtil.memPutInt(ptr + i * 4L, vertexOffset + ints.get(i));
                            }
                        }
                    }

                    var buffer = builder.build();
                    encoder.writeToBuffer(indexPool, buffer.byteBuffer(), finalOffset);
                }
            }

            return new UploadResult(finalOffset, mesh.drawState());
        };
    }

    public record UploadResult(int offset, DrawState drawState) { }
}
