package dev.compactmods.gander.runtime.baked.section;

import com.google.common.collect.Iterators;
import dev.compactmods.gander.render.baked.model.material.MaterialParent;

import dev.compactmods.gander.render.baked.model.BakedMesh;
import dev.compactmods.gander.render.baked.model.material.MaterialInstance;
import dev.compactmods.gander.runtime.baked.model.ModelRebaker;
import dev.compactmods.gander.runtime.baked.texture.AtlasIndexer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SectionBaker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SectionBaker.class);

    // TODO: these should encapsulate their details more...
    public record BakedSection(
        List<DrawCall> drawCalls,
        FloatBuffer atlas)
        implements AutoCloseable
    {
        @Override
        public void close() throws Exception
        {
            MemoryUtil.memFree(atlas);
            for (var call : drawCalls)
            {
                call.close();
            }
        }
    }

    public record DrawCall(
        RenderType renderType,
        BakedMesh mesh,
        int instanceCount,
        FloatBuffer transforms,
        int transformCount,
        IntBuffer textures,
        int textureCount)
        implements AutoCloseable
    {
        @Override
        public void close() throws Exception
        {
            MemoryUtil.memFree(transforms);
            MemoryUtil.memFree(textures);
        }
    }

    private record InstanceInfo(
        RenderType renderType,
        BakedMesh mesh,
        Set<MaterialInstance> materialInstances,
        Matrix4fc transform,
        int sectionRelativeX,
        int sectionRelativeY,
        int sectionRelativeZ)
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
            .collect(Collectors.groupingBy(InstanceInfo::renderType,
                Collectors.groupingBy(InstanceInfo::mesh)));

        if (renderPasses.isEmpty())
            return EMPTY;

        var drawCalls = new ArrayList<DrawCall>();
        var textureAtlases = new HashMap<ResourceLocation, List<ResourceLocation>>();
        for (var renderPass : renderPasses.entrySet())
        {
            for (var mesh : renderPass.getValue().entrySet())
            {
                drawCalls.add(buildBuffers(
                    renderPass.getKey(),
                    mesh.getKey(),
                    mesh.getValue(),
                    rebaker,
                    instance -> {
                        var atlas = textureAtlases.computeIfAbsent(
                            instance.parent().atlas(),
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

        var readOnlyAtlas = atlases.get(TextureAtlas.LOCATION_BLOCKS);
        var atlas = MemoryUtil.memAllocFloat(readOnlyAtlas.limit());
        atlas.put(readOnlyAtlas);
        atlas.flip();
        readOnlyAtlas.rewind();

        return new BakedSection(
            Collections.unmodifiableList(drawCalls),
            atlas);
    }

    private static DrawCall buildBuffers(
        RenderType renderType,
        BakedMesh mesh,
        Collection<InstanceInfo> instances,
        ModelRebaker rebaker,
        ToIntFunction<MaterialInstance> textureAtlasIndex)
    {
        var transforms = MemoryUtil.memAllocFloat(
            instances.size() * 4 * 4);

        var textureBuffer = MemoryUtil.memAllocInt(
            mesh.vertexCount() * instances.size());

        instances
            .forEach(it -> {
                for (var index : mesh.materialIndexes())
                {
                    var parent = mesh.materials().get(index);
                    var material = getMaterialInstance(
                        it.materialInstances(),
                        parent);

                    textureBuffer.put(textureAtlasIndex.applyAsInt(material));
                }

                var transform = new Matrix4f()
                    .translate(it.sectionRelativeX(),
                        it.sectionRelativeY(),
                        it.sectionRelativeZ())
                    .mul(it.transform());
                transform.get(transforms);
                transforms.position(transforms.position() + 16);
            });

        return new DrawCall(
            renderType,
            mesh,
            instances.size(),
            transforms.flip(),
            instances.size(),
            textureBuffer.flip(),
            mesh.vertexCount() * instances.size());
    }

    private static MaterialInstance getMaterialInstance(
        final Set<MaterialInstance> instances,
        final MaterialParent meshParent)
    {
        var iterator = instances.stream()
            .filter(x -> meshParent.name().equals(x.name()))
            .iterator();

        var material = MaterialInstance.MISSING;
        if (iterator.hasNext())
        {
            material = iterator.next();

            if (iterator.hasNext())
            {
                LOGGER.error("Found multiple potential parents for {}: {}",
                    meshParent.name(),
                    Iterators.toString(iterator));

                throw new IllegalStateException(
                    "More than one potential parent instance "
                    + "was found for "
                    + meshParent.name());
            }
        }

        return material;
    }

    private static Stream<InstanceInfo> modelsAt(
        Level level,
        BlockPos pos,
        ModelRebaker rebaker,
        RandomSource randomSource)
    {
        var blockState = level.getBlockState(pos);
        randomSource.setSeed(blockState.getSeed(pos));
        var modelRef = BlockModelShaper.stateToModelLocation(blockState);

        return rebaker.getArchetypes(modelRef)
            .meshes(randomSource)
            .map(it -> new InstanceInfo(
                it.renderType(),
                it.mesh(),
                it.materialInstances(),
                it.transform(),
                SectionPos.sectionRelative(pos.getX()),
                SectionPos.sectionRelative(pos.getY()),
                SectionPos.sectionRelative(pos.getZ())));
    }
}