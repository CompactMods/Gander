package dev.compactmods.gander.render.baked.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import dev.compactmods.gander.render.baked.BakedMesh;
import dev.compactmods.gander.render.baked.model.archetype.ArchetypeBaker;
import dev.compactmods.gander.render.baked.model.material.MaterialInstance;
import dev.compactmods.gander.render.baked.model.material.MaterialParent;
import dev.compactmods.gander.render.mixin.accessor.BlockModelAccessor;
import dev.compactmods.gander.render.mixin.accessor.ModelBakeryAccessor;
import dev.compactmods.gander.render.mixin.accessor.ModelManagerAccessor;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static Map<ModelResourceLocation, Multimap<MaterialParent, MaterialInstance>> MODEL_MATERIAL_INSTANCES;
    private static Map<ModelResourceLocation, Set<ModelResourceLocation>> MODEL_ARCHETYPES;
    private static Map<ModelResourceLocation, Set<ModelResourceLocation>> ARCHETYPE_MESHES;
    private static Map<ModelResourceLocation, BakedMesh> BAKED_MESHES;

    public static Set<ModelResourceLocation> getModelArchetypes(ModelResourceLocation model)
    {
        return MODEL_ARCHETYPES.getOrDefault(model, Set.of());
    }

    public static Set<ModelResourceLocation> getArchetypeModel(ModelResourceLocation archetype)
    {
        return ARCHETYPE_MESHES.getOrDefault(archetype, Set.of());
    }

    public static BakedMesh getArchetypeMesh(ModelResourceLocation archetypeModel)
    {
        return BAKED_MESHES.get(archetypeModel);
    }

    public static Multimap<MaterialParent, MaterialInstance> getMaterialInstances(ModelResourceLocation model)
    {
        return MODEL_MATERIAL_INSTANCES.getOrDefault(model, Multimaps.forMap(Map.of()));
    }

    public static void rebakeModels(ModelManagerAccessor manager, ProfilerFiller reloadProfiler)
    {
        try
        {
            var bakery = manager.getModelBakery();

            reloadProfiler.startTick();
            reloadProfiler.push("archetype_discovery");
            var archetypeSet = new HashSet<ModelResourceLocation>();
            var modelArchetypes = new HashMap<ModelResourceLocation, Set<ModelResourceLocation>>();
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
            var archetypeMeshes = new HashMap<ModelResourceLocation, Set<ModelResourceLocation>>();
            var bakedMeshes = new HashMap<ModelResourceLocation, BakedMesh>();
            for (var archetype : archetypeSet)
            {
                var model = bakery.getModel(archetype.id());
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("Baking archetype model {} of model type {}", archetype, model.getClass());

                var archetypes = ArchetypeBaker.bakeArchetypes(archetype, model);
                if (archetypes == null)
                {
                    LOGGER.warn("Archetype model {} returned no archetypes?", archetype);
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

            reloadProfiler.popPush("material_instances");
            var modelMaterials = new HashMap<ModelResourceLocation, Multimap<MaterialParent, MaterialInstance>>();
            for (var pair : manager.getBakedRegistry().entrySet())
            {
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("Baking material instances of model {}", pair.getKey());

                var unbakedModel = ((ModelBakeryAccessor)bakery).getTopLevelModels()
                    .get(pair.getKey());

                modelMaterials.put(pair.getKey(),
                    getMaterialInstances(bakery, pair.getKey(), unbakedModel)
                        .stream()
                        .collect(Multimaps.toMultimap(
                            MaterialInstance::material,
                            Function.identity(),
                            HashMultimap::create)));
            }

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Baked {} different material instances", modelMaterials.size());

            reloadProfiler.popPush("material_instances_cache");
            MODEL_MATERIAL_INSTANCES = Collections.unmodifiableMap(modelMaterials);

            reloadProfiler.pop();
            reloadProfiler.endTick();
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to rebake models", e);
        }
    }

    private static Set<ModelResourceLocation> getArchetypes(
        ModelResourceLocation model,
        ModelManagerAccessor manager,
        ModelBakery bakery)
    {
        var result = ImmutableSet.<ModelResourceLocation>builder();
        var visited = new HashSet<ResourceLocation>();
        var queue = new ArrayDeque<UnbakedModel>();

        var unbakedModel = ((ModelBakeryAccessor)bakery).getTopLevelModels().get(model);

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
                    result.add(new ModelResourceLocation(dep, "gander_archetype"));
                }
                // If it has geometry, it overrides any other parents
                else if (hasGeometry(childModel))
                {
                    result.add(new ModelResourceLocation(dep, "gander_archetype"));
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

    private static Set<MaterialInstance> getMaterialInstances(ModelBakery bakery, ModelResourceLocation key, UnbakedModel model)
    {
        switch (model)
        {
            case BlockModel block -> {
                return getBlockModelMaterials(key, block);
            }
            case MultiPart multiPart -> {
                return multiPart.getMultiVariants()
                    .stream()
                    .map(MultiVariant::getVariants)
                    .flatMap(List::stream)
                    .map(Variant::getModelLocation)
                    .map(bakery::getModel)
                    .map(it -> getMaterialInstances(bakery, key, it))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            }
            case MultiVariant multiVariant -> {
                return multiVariant.getVariants()
                    .stream()
                    .map(Variant::getModelLocation)
                    .map(bakery::getModel)
                    .map(it -> getMaterialInstances(bakery, key, it))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            }
            default -> throw new IllegalStateException("Unexpected value: " + model);
        }
    }

    private static Set<MaterialInstance> getBlockModelMaterials(ModelResourceLocation name, final BlockModel model)
    {
        var result = new HashSet<MaterialInstance>();
        Stream<Map.Entry<String, Either<net.minecraft.client.resources.model.Material, String>>> materials = Stream.of();
        for (var mdl = model; mdl != null; mdl = mdl.parent)
        {
            materials = Stream.concat(materials, mdl.textureMap.entrySet().stream());
        }

        var fullMaterialMap = materials.collect(Collectors.toSet());

        var materialParents = getModelArchetypes(name)
            .stream()
            .map(ModelRebaker::getArchetypeModel)
            .flatMap(Set::stream)
            .map(ModelRebaker::getArchetypeMesh)
            .map(BakedMesh::materials)
            .flatMap(List::stream)
            .collect(Collectors.toSet());

        var parentsByName = new HashMap<String, MaterialParent>();
        materialParents.forEach(material -> {
            parentsByName.put(material.name(), material);
        });

        fullMaterialMap.stream()
            .map(it -> Pair.of(it.getKey(), it.getValue().left().orElse(null)))
            .filter(it -> it.getSecond() != null)
            .forEach(material -> {
                parentsByName.putIfAbsent(
                    material.getFirst(),
                    new MaterialParent(
                        material.getFirst(),
                        material.getSecond().atlasLocation(),
                        material.getSecond().texture()));
            });

        var usedMaterials = fullMaterialMap
            .stream()
            .map(it -> it.getValue()
                .map(
                    left -> Map.entry(
                        it.getKey(),
                        checkMaterials(
                            it.getKey(),
                            parentsByName.getOrDefault(
                                it.getKey(),
                                MaterialParent.MISSING),
                            left)),
                    ref -> Map.entry(
                        it.getKey(),
                        parentsByName.getOrDefault(
                            ref,
                            MaterialParent.MISSING))))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        for (var usedMaterial : usedMaterials)
        {
            var parent = parentsByName.get(usedMaterial.getKey());
            var resolved = usedMaterial.getValue();
            var overridenTexture = resolved.defaultValue();

            result.add(new MaterialInstance(parent, overridenTexture));
        }

        return result;
    }

    private static MaterialParent checkMaterials(
        String name,
        MaterialParent material,
        Material vanilla)
    {
        if (material == null)
        {
            return new MaterialParent(name, vanilla.atlasLocation(), vanilla.texture());
        }

        // We only care if the atlas matches, because if it doesn't that's a problem.
        Preconditions.checkArgument(vanilla.atlasLocation().equals(material.atlas()));
        return material;
    }
}
