package dev.compactmods.gander.render.geometry;

import java.util.Map;

import com.mojang.blaze3d.vertex.MeshData;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.phys.AABB;

public record BakedLevelSection(SectionBufferBuilderPack sectionBuffer,
                                Map<ChunkSectionLayer, MeshData> layers,
                                AABB chunkArea) {
}
