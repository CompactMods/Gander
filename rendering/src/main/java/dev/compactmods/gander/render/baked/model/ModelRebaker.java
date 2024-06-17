package dev.compactmods.gander.render.baked.model;

import com.google.common.collect.ImmutableSet;
import dev.compactmods.gander.render.baked.BakedMesh;
import dev.compactmods.gander.render.baked.model.ModelVertices.ModelVertex;
import dev.compactmods.gander.render.mixin.accessor.BlockModelAccessor;
import dev.compactmods.gander.render.mixin.accessor.CompositeModelAccessor;
import dev.compactmods.gander.render.mixin.accessor.ModelManagerAccessor;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rebakes meshes to not use an interleaved format for their data. Leaves the
 * original data as-is for mods which rely on the interleaved data for their own
 * purposes.
 * <p>
 * Thanks Mojank.
 */
public final class ModelRebaker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelRebaker.class);

    private ModelRebaker() { }

    private static Map<ResourceLocation, MaterialInstance> MODEL_MATERIAL_INSTANCES;
    private static Map<ResourceLocation, Set<ResourceLocation>> MODEL_ARCHETYPES;
    private static Map<ResourceLocation, Set<ModelResourceLocation>> ARCHETYPE_MESHES;
    private static Map<ModelResourceLocation, BakedMesh> BAKED_MESHES;

    public static Set<ResourceLocation> getModelArchetypes(ResourceLocation model)
    {
        return MODEL_ARCHETYPES.get(model);
    }

    public static Set<ModelResourceLocation> getArchetypeMeshes(ResourceLocation archetype)
    {
        return ARCHETYPE_MESHES.get(archetype);
    }

    public static BakedMesh getMesh(ModelResourceLocation mesh)
    {
        return BAKED_MESHES.get(mesh);
    }

    public static void rebakeModels(ModelManagerAccessor manager, ProfilerFiller reloadProfiler)
    {
        try
        {
            var bakery = manager.getModelBakery();

            reloadProfiler.startTick();
            reloadProfiler.push("archetype_discovery");
            var archetypeSet = new HashSet<ResourceLocation>();
            var modelArchetypes = new HashMap<ResourceLocation, Set<ResourceLocation>>();
            for (var pair : manager.getBakedRegistry().entrySet())
            {
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("Discovering archetypes of model {} ", pair.getKey());

                reloadProfiler.push(pair.getKey().toString());
                var archetypes = getArchetypes(pair.getKey(), manager, bakery);
                modelArchetypes.put(pair.getKey(), archetypes);
                archetypeSet.addAll(archetypes);
                reloadProfiler.pop();
            }

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Computed {} different archetype models for all models", archetypeSet.size());

            reloadProfiler.popPush("archetype_baking");
            var archetypeMeshes = new HashMap<ResourceLocation, Set<ModelResourceLocation>>();
            var bakedMeshes = new HashMap<ModelResourceLocation, BakedMesh>();
            for (var archetype : archetypeSet)
            {
                var model = bakery.getModel(archetype);
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("Baking archetype model {} of model type {}", archetype, model.getClass());

                var archetypes = bakeArchetypes(archetype, model);
                if (archetypes == null)
                {
                    LOGGER.warn("Archetype model {} returned no archetypes to draw?", archetype);
                    continue;
                }

                bakedMeshes.putAll(archetypes);
                archetypeMeshes.put(archetype, archetypes.keySet());
            }

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Baked {} different archetypes", archetypeMeshes.size());

            reloadProfiler.popPush("archetype_cache");
            MODEL_ARCHETYPES = Collections.unmodifiableMap(modelArchetypes);
            ARCHETYPE_MESHES = Collections.unmodifiableMap(archetypeMeshes);
            BAKED_MESHES = Collections.unmodifiableMap(bakedMeshes);

            reloadProfiler.pop();
            reloadProfiler.endTick();
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to rebake models", e);
        }
    }

    private static Set<ResourceLocation> getArchetypes(ResourceLocation model, ModelManagerAccessor manager, ModelBakery bakery)
    {
        var result = ImmutableSet.<ResourceLocation>builder();
        var visited = new HashSet<ResourceLocation>();
        var queue = new ArrayDeque<UnbakedModel>();

        var unbakedModel = bakery.getModel(model);

        // If the model itself has geometry, it is its own archetype.
        if (hasGeometry(unbakedModel))
        {
            return Set.of(model);
        }

        // Otherwise, we need to search its parents
        queue.add(unbakedModel);
        while (!queue.isEmpty())
        {
            var first = queue.removeFirst();
            for (var dep : first.getDependencies())
            {
                var childModel = bakery.getModel(dep);
                // TODO: figure out if this is what we want to do here
                if (childModel == manager.getMissingModel()) continue;

                var deps = childModel.getDependencies();
                // If this rootmost model has geometry, it is an archetype
                if (deps.isEmpty() && hasGeometry(childModel))
                {
                    result.add(dep);
                }
                // If it has geometry, it overrides any other parents
                else if (hasGeometry(childModel))
                {
                    result.add(dep);
                }
                // Otherwise, check we haven't seen it before.
                else if (visited.add(dep))
                {
                    queue.addLast(childModel);
                }
            }
        }

        return result.build();
    }

    private static boolean hasGeometry(UnbakedModel model)
    {
        // If the child model has its own geometry, it overrides any
        // parents.
        if (model instanceof BlockModel blockModel)
        {
            if (blockModel.customData.hasCustomGeometry())
            {
                return true;
            }

            var accessor = (BlockModelAccessor)blockModel;
            if (!accessor.getOwnElements().isEmpty())
            {
                return true;
            }
        }

        return false;
    }

    private static Map<ModelResourceLocation, BakedMesh> bakeArchetypes(ResourceLocation originalName, UnbakedModel model)
    {
        switch (model)
        {
            case BlockModel block -> {
                if (block.customData.hasCustomGeometry())
                    return bakeCustomGeometry(originalName, block.customData.getCustomGeometry());
                else
                    return bakeElements(originalName, block.getElements());
            }
            default -> throw new IllegalStateException("Unexpected value: " + model);
        }
    }

    private static Map<ModelResourceLocation, BakedMesh> bakeCustomGeometry(ResourceLocation originalName, IUnbakedGeometry<?> geometry)
    {
        if (!(geometry instanceof CompositeModel composite))
        {
            LOGGER.error("Unsupported custom geometry type {}", geometry.getClass());
            return null;
        }
        var accessor = (CompositeModelAccessor)composite;

        return accessor.getChildren().entrySet()
            .stream()
            .map(it -> {
                var name = it.getValue().name;
                if (name == null || name.isBlank())
                    name = it.getKey();

                return bakeArchetypes(
                    computeModelName(originalName, "composite_child/" + name),
                    it.getValue());
            })
            .flatMap(it -> it.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<ModelResourceLocation, BakedMesh> bakeElements(
        ResourceLocation originalName,
        List<BlockElement> elements)
    {
        var archetypeName = computeModelName(originalName);
        var allFaces = elements.stream()
            .map(ModelRebaker::bakeElement)
            .reduce(ModelVertices::combine)
            .stream()
            .flatMap(it -> it.faces().stream())
            .toList();

        if (allFaces.isEmpty() && LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Could not bake elements for mesh {}", archetypeName);
            return null;
        }

        var deduplicatedVertices = allFaces.stream()
            .flatMap(Collection::stream)
            .distinct()
            .toList();

        var vertexBuffer = FloatBuffer.allocate(deduplicatedVertices.size() * 3);
        var normalBuffer = FloatBuffer.allocate(deduplicatedVertices.size() * 3);
        var uvBuffer = FloatBuffer.allocate(deduplicatedVertices.size() * 2);
        for (var vertex : deduplicatedVertices)
        {
            vertexBuffer.put(vertex.position().x());
            vertexBuffer.put(vertex.position().y());
            vertexBuffer.put(vertex.position().z());
            normalBuffer.put(vertex.normal().x());
            normalBuffer.put(vertex.normal().y());
            normalBuffer.put(vertex.normal().z());
            uvBuffer.put(vertex.uv().x());
            uvBuffer.put(vertex.uv().y());
        }

        // There are two triangles per face, so we need to double this.
        var indexBuffer = ByteBuffer.allocate(allFaces.stream().mapToInt(Set::size).sum() * 2);
        var tempBuffer = new ModelVertex[4];
        for (var face : allFaces)
        {
            // Face vertices built by ModelVertices.compute are in this order:
            // 0---1
            // |   |
            // 2---3
            // Since we're in CCW normal order, this means this order:
            // 0-<-1 1
            // |  / /|
            // v ^ v ^
            // |/ /  |
            // 2 2->-3
            // So we want the vertices in this order:
            // 021
            // 312
            var vertices = face.toArray(tempBuffer);
            if (vertices.length != 4)
                LOGGER.error("Face contains more than 4 vertices. This needs to be implemented.");

            indexBuffer.put((byte)deduplicatedVertices.indexOf(vertices[0]));
            indexBuffer.put((byte)deduplicatedVertices.indexOf(vertices[2]));
            indexBuffer.put((byte)deduplicatedVertices.indexOf(vertices[1]));
            indexBuffer.put((byte)deduplicatedVertices.indexOf(vertices[3]));
            indexBuffer.put((byte)deduplicatedVertices.indexOf(vertices[1]));
            indexBuffer.put((byte)deduplicatedVertices.indexOf(vertices[2]));
        }

        return Map.of(archetypeName, new BakedMesh(
            vertexBuffer.flip(),
            normalBuffer.flip(),
            uvBuffer.flip(),
            indexBuffer.flip()));
    }

    private static ModelResourceLocation computeModelName(ResourceLocation original)
    {
        if (original instanceof ModelResourceLocation model)
        {
            if (model.getVariant().isEmpty())
                return new ModelResourceLocation(
                    model.getNamespace(),
                    model.getPath(),
                    "gander_archetype");
            else if (model.getVariant().startsWith("gander_archetype"))
                return model;
            else
                return new ModelResourceLocation(
                    model.getNamespace(),
                    model.getPath(),
                    "gander_archetype/" + model.getVariant());
        }

        return new ModelResourceLocation(
            original.getNamespace(),
            original.getPath(),
            "gander_archetype");
    }

    private static ModelResourceLocation computeModelName(ResourceLocation original, String variant)
    {
        if (original instanceof ModelResourceLocation model)
        {
            if (model.getVariant().isEmpty())
                return new ModelResourceLocation(
                    model.getNamespace(),
                    model.getPath(),
                    "gander_archetype/" + variant);
            else if (model.getVariant().startsWith("gander_archetype"))
                return new ModelResourceLocation(
                    model.getNamespace(),
                    model.getPath(),
                    model.getVariant() + "/" + variant);
            else
                return new ModelResourceLocation(
                    model.getNamespace(),
                    model.getPath(),
                    "gander_archetype/" + model.getVariant() + "/" + variant);
        }

        return new ModelResourceLocation(
            original.getNamespace(),
            original.getPath(),
            "gander_archetype/" + variant);
    }

    private static ModelVertices bakeElement(BlockElement element)
    {
        var rotation = computeRotation(element.rotation);
        var normals = ModelNormals.compute(element, rotation);
        var uvs = ModelUvs.compute(element);
        return ModelVertices.compute(element, rotation, normals, uvs);
    }

    private static final Quaternionfc NO_ROTATION = new Quaternionf();
    private static Quaternionfc computeRotation(@Nullable BlockElementRotation rotation)
    {
        // If there was no rotation specified, we don't want to do any maths.
        if (rotation == null) return NO_ROTATION;

        var axis = new Vector3f();
        switch (rotation.axis()) {
            case X -> axis.set(1, 0, 0);
            case Y -> axis.set(0, 1, 0);
            case Z -> axis.set(0, 0, 1);
        };
        return new Quaternionf().rotateAxis(
            (float)Math.toRadians(rotation.angle()),
            axis);
    }
}
