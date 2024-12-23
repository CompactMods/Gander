package dev.compactmods.gander.runtime.baked;

import com.google.common.collect.Maps;

import dev.compactmods.gander.runtime.mixin.accessor.StitchResultAccessor;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Bakes texture atlases into a UV buffer
 */
public final class AtlasIndex implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AtlasIndex.class);

    private final Map<ResourceLocation, IndexEntry> _atlases;

    public AtlasIndex(Map<ResourceLocation, ResourceLocation> atlases)
    {
        // TODO: this is technically unnecessary, but it might be useful in the future...
        _atlases = atlases.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                pair -> new IndexEntry(new AtlasIndices())));
    }

    public Map<ResourceLocation, CompletableFuture<IndexResult>> bakeAtlasIndices(
        Map<ResourceLocation, CompletableFuture<AtlasSet.StitchResult>> stitchedAtlases,
        Executor backgroundExecutor)
    {
        return Maps.transformEntries(_atlases,
            (key, entry) -> stitchedAtlases.get(key).thenApplyAsync(
                result -> bakeAtlas(key, result, entry), backgroundExecutor));
    }

    private IndexResult bakeAtlas(
        ResourceLocation atlas,
        AtlasSet.StitchResult stitchResult,
        IndexEntry indexEntry)
    {
        if (LOGGER.isTraceEnabled())
            LOGGER.trace("Baking atlas indices for {}", atlas);

        var preparations = ((StitchResultAccessor)stitchResult).getPreparations();
        var coordinates = preparations.regions()
            .entrySet()
            .stream()
            .map(entry -> Map.entry(entry.getKey(),
                new Vector4f(
                    entry.getValue().getU0(), entry.getValue().getV0(),
                    entry.getValue().getU1(), entry.getValue().getV1())))
            .toList();

        var buffer = MemoryUtil.memAllocFloat(coordinates.size() * 4);
        for (var entry : coordinates)
        {
            buffer.put(entry.getValue().x());
            buffer.put(entry.getValue().y());
            buffer.put(entry.getValue().z());
            buffer.put(entry.getValue().w());
        }

        if (LOGGER.isTraceEnabled())
            LOGGER.trace("Baked {} different sprites", coordinates.size());

        return new IndexResult(indexEntry,
            buffer.flip(),
            coordinates.stream()
                .map(Map.Entry::getKey)
                .toList());
    }

    public AtlasIndices getAtlasIndices(ResourceLocation atlas)
    {
        return _atlases.get(atlas).indices();
    }

    @Override
    public void close()
    {
        _atlases.values().forEach(IndexEntry::close);
    }

    record IndexEntry(AtlasIndices indices)
        implements AutoCloseable
    {
        void apply(FloatBuffer buffer, List<ResourceLocation> keys)
        {
            indices._buffer = buffer;
            indices._keys = keys;
            indices._missingTextureIndex = keys.indexOf(MissingTextureAtlasSprite.getLocation());
        }

        @Override
        public void close()
        {
            indices.close();
        }
    }

    public static class AtlasIndices
    {
        private FloatBuffer _buffer;
        private List<ResourceLocation> _keys;
        private int _missingTextureIndex;

        private AtlasIndices()
        {
            _buffer = null;
            _keys = null;
            _missingTextureIndex = -1;
        }

        public FloatBuffer acquire()
        {
            return _buffer;
        }

        public int getIndexOf(ResourceLocation texture)
        {
            var index = _keys.indexOf(texture);

            return (index >= 0)
                ? index
                : _missingTextureIndex;
        }

        void close()
        {
            if (_buffer != null)
            {
                MemoryUtil.memFree(_buffer);
                _buffer = null;
                _keys = null;
                _missingTextureIndex = -1;
            }
        }
    }

    public static class IndexResult
    {
        private final IndexEntry _entry;

        private final FloatBuffer _buffer;
        private final List<ResourceLocation> _keys;

        private IndexResult(IndexEntry entry,
            FloatBuffer buffer,
            List<ResourceLocation> keys)
        {
            _entry = entry;
            _buffer = buffer;
            _keys = keys;
        }

        public void upload()
        {
            _entry.apply(_buffer, _keys);
        }
    }
}
