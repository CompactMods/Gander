package dev.compactmods.gander.render.geometry;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;

import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MultiPassGeometryUploader {

    private final Map<RenderType, MultiPassGpuBuffer> buffers = new HashMap<>();

    public SectionRenderPhase makeUploadTask(RenderType type, MeshData meshData) {
        MultiPassGpuBuffer buffer = buffers.computeIfAbsent(type, t -> MultiPassGpuBuffer.create(t, meshData));
        return new SectionRenderPhase(type, buffer);
    }

    public record MultiPassGpuBuffer(RenderType renderType, MeshData meshData, GpuBuffer vertexBuffer, GpuBuffer indexBuffer) {

        public static MultiPassGpuBuffer create(RenderType type, MeshData meshData) {
            GpuBuffer gpubuffer = createGpuBufferForMeshData(type, meshData);
            GpuBuffer indexedBuffer = meshData.indexBuffer() != null
                ? createGpuBufferForIndexedMeshData(type, meshData)
                : null;

            return new MultiPassGpuBuffer(type, meshData, gpubuffer, indexedBuffer);
        }

        public VertexFormat.IndexType indexType() {
            return meshData.drawState().indexType();
        }

        public int indexCount() {
            return meshData.drawState().indexCount();
        }

        private static @NotNull GpuBuffer createGpuBufferForIndexedMeshData(RenderType renderType, MeshData meshData) {
            return RenderSystem.getDevice()
                .createBuffer(
                    () -> "GANDER/MOJANG PLS - layer: %s;".formatted(renderType.getName()),
                    BufferType.INDICES,
                    BufferUsage.STATIC_WRITE,
                    Objects.requireNonNull(meshData.indexBuffer())
                );
        }

        private static @NotNull GpuBuffer createGpuBufferForMeshData(RenderType renderType, MeshData meshData) {
            return RenderSystem.getDevice()
                .createBuffer(
                    () -> "GANDER/MOJANG PLS - layer: %s;".formatted(renderType.getName()),
                    BufferType.VERTICES,
                    BufferUsage.STATIC_WRITE,
                    meshData.vertexBuffer()
                );
        }
    }

    public record SectionRenderPhase(RenderType renderType, MultiPassGpuBuffer buffer) {

        // from SectionRenderDispatcher
        public CompletableFuture<Void> upload(final CommandEncoder encoder, MeshData meshData) {
            return CompletableFuture.runAsync(() -> {
                if (buffer.vertexBuffer.size() < meshData.vertexBuffer().remaining()) {
                    buffer.vertexBuffer.close();
                } else if (!buffer.vertexBuffer.isClosed()) {
                    encoder.writeToBuffer(buffer.vertexBuffer, meshData.vertexBuffer(), 0);
                }

                if (meshData.indexBuffer() != null) {
                    if (buffer.indexBuffer != null
                        && buffer.indexBuffer.size() >= meshData.indexBuffer().remaining()) {
                        if (!buffer.indexBuffer.isClosed()) {
                            encoder.writeToBuffer(buffer.indexBuffer, meshData.indexBuffer(), 0);
                        }
                    } else {
                        if (buffer.indexBuffer != null) {
                            buffer.indexBuffer.close();
                        }
                    }
                } else if (buffer.indexBuffer != null) {
                    buffer.indexBuffer.close();
                }

                meshData.close();
            });
        }
    }


}
