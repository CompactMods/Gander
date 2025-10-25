package dev.compactmods.gander.render.geometry;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.world.phys.AABB;

public record BakedLevelSection(SectionBufferBuilderPack sectionBuffer,
                                net.minecraft.client.renderer.chunk.SectionCompiler.Results layers,
                                AABB chunkArea) {
}
