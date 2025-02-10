package dev.compactmods.gander.render.geometry;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;

import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;

import net.minecraft.world.phys.AABB;

import org.joml.Vector3f;

import java.util.Map;
import java.util.function.Function;

public record BakedLevelSection(SectionBufferBuilderPack blockBuilders,
                                SectionBufferBuilderPack fluidBuilders,
                                Map<RenderType, VertexBuffer> blockBuffers,
                                Map<RenderType, VertexBuffer> fluidBuffers,
                                Map<RenderType, MeshData.SortState> blockSortStates,
                                Map<RenderType, MeshData.SortState> fluidSortStates,
                                AABB blockBoundaries) {

    public void resortTranslucency(Vector3f cameraPosition) {
        var vertexSorting = VertexSorting.byDistance(cameraPosition.x, cameraPosition.y, cameraPosition.z);
        resortTranslucency(vertexSorting, blockBuilders, blockBuffers, blockSortStates);
        resortTranslucency(vertexSorting, fluidBuilders, fluidBuffers, fluidSortStates);
    }

    private void resortTranslucency(
        VertexSorting vertexSorting,
        SectionBufferBuilderPack pack,
        Map<RenderType, VertexBuffer> buffers,
        Map<RenderType, MeshData.SortState> sortStates) {

        sortStates.forEach((type, state) -> {
            var result = state.buildSortedIndexBuffer(pack.buffer(type), vertexSorting);
            if (result == null)
                return;

            var buffer = buffers.get(type);
            if(buffer != null) {
                buffer.bind();
                buffer.uploadIndexBuffer(result);
                VertexBuffer.unbind();
            }
        });
    }
}
