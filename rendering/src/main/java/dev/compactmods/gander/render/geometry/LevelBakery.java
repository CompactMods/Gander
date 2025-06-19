package dev.compactmods.gander.render.geometry;

import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.core.math.WorldMath;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.AABB;

import org.joml.Vector3f;

import java.util.Map;
import java.util.stream.Collectors;

public class LevelBakery {

    public static BakedLevel bakeVertices(Level level, AABB blockBoundaries, Vector3f cameraPosition) {

        final var allSections = WorldMath.sectionPositions(level, blockBoundaries)
            .collect(Collectors.toSet());

        Minecraft mc = Minecraft.getInstance();

        if (!GanderGeometryHelper.initialized())
            GanderGeometryHelper.setup();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        Map<Long, BakedLevelSection> bakedSections = new Long2ObjectOpenHashMap<>();
        try {
            final var sorting = VertexSorting.byDistance(cameraPosition.x, cameraPosition.y, cameraPosition.z);

            RenderRegionCache regionCache = new RenderRegionCache();
            for (var sectionPos : allSections) {
                final AABB sectionAABB = WorldMath.sectionAABB(sectionPos);

                final var renderChunk = regionCache.createRegion(level, sectionPos.asLong());
                if (renderChunk == null) {
                    // Empty section - see createRegion
                    continue;
                }

                final SectionBufferBuilderPack bufferPack = new SectionBufferBuilderPack();
                final var compileResults = GanderGeometryHelper.SECTION_COMPILER
                    .compile(sectionPos, renderChunk, sorting, bufferPack);

                final var bakedSection = new BakedLevelSection(bufferPack,
                    compileResults.renderedLayers,
                    sectionAABB);

                bakedSections.put(sectionPos.asLong(), bakedSection);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new BakedLevel(level, blockBoundaries, bakedSections);
    }
}
