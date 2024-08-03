package dev.compactmods.gander.runtime.baked.section;

import com.mojang.math.Transformation;
import dev.compactmods.gander.render.baked.model.BakedMesh;
import dev.compactmods.gander.render.baked.model.DisplayableMesh;
import dev.compactmods.gander.render.baked.model.material.MaterialInstance;
import dev.compactmods.gander.runtime.baked.model.ModelRebaker;
import dev.compactmods.gander.runtime.baked.texture.AtlasIndexer;
import dev.compactmods.gander.runtime.mixin.accessor.TransformationAccessor;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SectionBaker
{
    public record BakedSection(
        List<DrawCall> drawCalls,
        FloatBuffer atlas)
    { }

    public record DrawCall(
        BakedMesh mesh,
        int instanceCount,
        FloatBuffer transforms,
        int transformCount,
        IntBuffer textures,
        int textureCount)
    { }

    private SectionBaker() { }

    public static final BakedSection EMPTY = new BakedSection(null, null);

    public static BakedSection bake(
        Level level,
        SectionPos section,
        ModelRebaker rebaker,
        AtlasIndexer indexer)
    {
        var randomSource = RandomSource.create();
        // The map of render passes, to the map of meshes
        var renderPasses = section.blocksInside()
            .flatMap(pos -> modelsAt(level, pos, rebaker, randomSource))
            .collect(Collectors.groupingBy(DisplayableMesh::renderType,
                Collectors.groupingBy(DisplayableMesh::mesh)));

        if (renderPasses.isEmpty())
            return EMPTY;

        var drawCalls = new ArrayList<DrawCall>();
        var textureAtlases = new HashMap<ResourceLocation, List<ResourceLocation>>();
        for (var renderPass : renderPasses.entrySet())
        {
            for (var mesh : renderPass.getValue().entrySet())
            {
                drawCalls.add(buildBuffers(
                    mesh.getKey(),
                    mesh.getValue(),
                    rebaker,
                    instance -> {
                        var atlas = textureAtlases.computeIfAbsent(
                            instance.material().atlas(),
                            indexer::getAtlasIndexes);

                        var index = atlas.indexOf(
                            instance.getEffectiveTexture());
                        if (index < 0)
                            index = atlas.indexOf(
                                MissingTextureAtlasSprite.getLocation());

                        // This is a bug.
                        if (index < 0)
                            throw new IllegalStateException(
                                "Couldn't locate missing texture in atlas");

                        return index;
                    }));
            }
        }

        var atlases = textureAtlases.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                it -> indexer.getAtlasBuffer(it.getKey())));

        if (atlases.size() > 1)
            throw new IllegalStateException("Not yet supported");

        return new BakedSection(
            Collections.unmodifiableList(drawCalls),
            atlases.get(TextureAtlas.LOCATION_BLOCKS));
    }

    private static DrawCall buildBuffers(
        BakedMesh mesh,
        Collection<DisplayableMesh> instances,
        ModelRebaker rebaker,
        ToIntFunction<MaterialInstance> textureAtlasIndex)
    {
        var transforms = FloatBuffer.allocate(
            instances.size() * 4 * 4);
        // N.B. std140 layout requires 16-byte alignment
        var textureBuffer = IntBuffer.allocate(
            (mesh.vertexCount() * instances.size()) * 4);

        instances
            .forEach(it -> {
                for (var index : it.mesh().materialIndexes())
                {
                    var parent = it.mesh().materials().get(index);
                    var potentials = it.materialInstances().get(parent);
                    var iterator = potentials.iterator();

                    var material = MaterialInstance.MISSING;
                    if (iterator.hasNext())
                    {
                        material = iterator.next();
                        if (iterator.hasNext())
                        {
                            throw new IllegalStateException(
                                "More than one potential material instance "
                                + "was found for "
                                + parent.name());
                        }
                    }

                    // Extra padding bytes here are necessary for std140 alignment
                    textureBuffer.put(textureAtlasIndex.applyAsInt(material));
                    textureBuffer.put(0);
                    textureBuffer.put(0);
                    textureBuffer.put(0);
                }

                var transform = it.transform();
                writeTransform(transforms, transform);
            });

        return new DrawCall(
            mesh,
            instances.size(),
            transforms.flip(),
            instances.size(),
            textureBuffer.flip(),
            mesh.vertexCount() * instances.size());
    }

    private static FloatBuffer writeTransform(FloatBuffer buffer,
        @NotNull Transformation transformation)
    {
        var result = buffer.slice();
        buffer.position(buffer.position() + 16);
        var accessor = (TransformationAccessor)(Object)transformation;
        var matrix = accessor.gander$matrix();

        result.put(matrix.m00())
            .put(matrix.m01())
            .put(matrix.m02())
            .put(matrix.m03())
            .put(matrix.m10())
            .put(matrix.m11())
            .put(matrix.m12())
            .put(matrix.m13())
            .put(matrix.m20())
            .put(matrix.m21())
            .put(matrix.m22())
            .put(matrix.m23())
            .put(matrix.m30())
            .put(matrix.m31())
            .put(matrix.m32())
            .put(matrix.m33());

        return result.flip();
    }

    private static Stream<DisplayableMesh> modelsAt(
        Level level,
        BlockPos pos,
        ModelRebaker rebaker,
        RandomSource randomSource)
    {
        var blockState = level.getBlockState(pos);
        randomSource.setSeed(blockState.getSeed(pos));
        var modelPos = BlockModelShaper.stateToModelLocation(blockState);
        return rebaker.getArchetypes(modelPos)
            .meshes(randomSource)
            .map(it -> new DisplayableMesh(
                it.name(),
                it.mesh(),
                it.renderType(),
                it.transform(),
                it.weight(),
                it.materialInstanceSupplier()));
    }
}