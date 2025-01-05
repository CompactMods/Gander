package dev.compactmods.gander.render.geometry;

import com.mojang.blaze3d.vertex.MeshData;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.RenderType;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import org.joml.Vector3f;

import java.util.Map;

public final class BakedLevel {
    private final Level originalLevel;
    private final SectionBufferBuilderPack blockBuilders;
    private final SectionBufferBuilderPack fluidBuilders;
    private final Map<RenderType, VertexBuffer> blockBuffers;
    private final Map<RenderType, VertexBuffer> fluidBuffers;
    private final Map<RenderType, MeshData.SortState> blockSortStates;
    private final Map<RenderType, MeshData.SortState> fluidSortStates;
    private final BoundingBox blockBoundaries;

    public BakedLevel(Level originalLevel,
                      SectionBufferBuilderPack blockBuilders,
                      SectionBufferBuilderPack fluidBuilders,
                      Map<RenderType, VertexBuffer> blockBuffers,
                      Map<RenderType, VertexBuffer> fluidBuffers,
                      Map<RenderType, MeshData.SortState> blockSortStates,
                      Map<RenderType, MeshData.SortState> fluidSortStates,
                      BoundingBox blockBoundaries) {
        this.originalLevel = originalLevel;
        this.blockBuilders = blockBuilders;
        this.fluidBuilders = fluidBuilders;
        this.blockBuffers = blockBuffers;
        this.fluidBuffers = fluidBuffers;
        this.blockSortStates = blockSortStates;
        this.fluidSortStates = fluidSortStates;
        this.blockBoundaries = blockBoundaries;
    }

    public void resortTranslucency(Vector3f cameraPosition) {
        // FIXME - This is broken somehow, causes a black screen
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
            buffer.uploadIndexBuffer(result);
            VertexBuffer.unbind();
        });
    }

    public Level originalLevel() {
        return originalLevel;
    }

    public Map<RenderType, VertexBuffer> blockRenderBuffers() {
        return blockBuffers;
    }

    public Map<RenderType, VertexBuffer> fluidRenderBuffers() {
        return fluidBuffers;
    }

    public BoundingBox blockBoundaries() {
        return blockBoundaries;
    }
}
