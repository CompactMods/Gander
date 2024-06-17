package dev.compactmods.gander.render.baked.texture;

import dev.compactmods.gander.render.mixin.accessor.StitchResultAccessor;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Bakes texture atlases into a UV buffer
 */
public final class AtlasBaker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AtlasBaker.class);

    private AtlasBaker() { }

    private static Map<ResourceLocation, AtlasIndices> ATLASES;

    public static FloatBuffer getAtlasBuffer(ResourceLocation atlas)
    {
        return ATLASES.get(atlas).buffer();
    }

    public static List<ResourceLocation> getAtlasIndexes(ResourceLocation atlas)
    {
        return ATLASES.get(atlas).indexes();
    }

    public static Map<ResourceLocation, CompletableFuture<AtlasSet.StitchResult>> bakeAtlases(
        Map<ResourceLocation, CompletableFuture<AtlasSet.StitchResult>> original,

        Executor backgroundExecutor)
    {
        var map = original.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Entry::getKey,
                value -> value.getValue()
                    .thenApplyAsync(result -> bakeAtlas(value.getKey(), result), backgroundExecutor)));

        var result = CompletableFuture.allOf(map.values().toArray(CompletableFuture[]::new))
            .thenApply(unused -> map.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().join())))
            .thenAcceptAsync(x -> ATLASES = x, backgroundExecutor);

        return original.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Entry::getKey,
                entry -> result.thenCompose(unused -> entry.getValue())));
    }

    private static AtlasIndices bakeAtlas(
        ResourceLocation atlas,
        AtlasSet.StitchResult stitchResult)
    {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Baking atlas {}", atlas);

        var preparations = ((StitchResultAccessor)stitchResult).getPreparations();
        var coordinates = preparations.regions()
            .entrySet()
            .stream()
            .map(entry -> Map.entry(entry.getKey(),
                new Vector4f(
                    entry.getValue().getU0(), entry.getValue().getV0(),
                    entry.getValue().getU1(), entry.getValue().getV1())))
            .toList();

        var buffer = FloatBuffer.allocate(coordinates.size() * 4);
        for (var entry : coordinates)
        {
            buffer.put(entry.getValue().x());
            buffer.put(entry.getValue().y());
            buffer.put(entry.getValue().z());
            buffer.put(entry.getValue().w());
        }

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Baked {} different sprites", coordinates.size());

        return new AtlasIndices(
            buffer.flip(),
            coordinates.stream()
                .map(Map.Entry::getKey)
                .toList());
    }
}
