package dev.compactmods.gander.render.geometry;

import java.util.Map;

import com.mojang.blaze3d.vertex.MeshData;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;

public record BakedLevelSection(SectionBufferBuilderPack sectionBuffer,
                                Map<RenderType, MeshData> meshData, net.minecraft.world.phys.AABB chunkArea) {
}
