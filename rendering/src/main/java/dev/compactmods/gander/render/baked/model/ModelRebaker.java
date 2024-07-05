package dev.compactmods.gander.render.baked.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mojang.math.Transformation;
import dev.compactmods.gander.render.baked.model.archetype.ArchetypeBaker;
import dev.compactmods.gander.render.baked.model.archetype.ArchetypeComponent;
import dev.compactmods.gander.render.baked.model.material.MaterialBaker;
import dev.compactmods.gander.render.baked.model.material.MaterialInstance;
import dev.compactmods.gander.render.baked.model.material.MaterialParent;
import dev.compactmods.gander.render.mixin.accessor.BlockModelShaperAccessor;
import dev.compactmods.gander.render.mixin.accessor.ModelBakeryAccessor;
import dev.compactmods.gander.render.mixin.accessor.ModelManagerAccessor;
import dev.compactmods.gander.render.mixin.accessor.MultiPartAccessor;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    // Map of model -> material instances
    private static Map<ModelResourceLocation, Multimap<MaterialParent, MaterialInstance>> MODEL_MATERIAL_INSTANCES;
    // Multimap of model -> baked mesh
    private static Multimap<ModelResourceLocation, DisplayableMesh> BAKED_MODEL_MESHES;

    public static Collection<DisplayableMesh> getArchetypeMeshes(ModelResourceLocation model)
    {
        // This SHOULD be unmodifiable, but just in case...
        return Collections.unmodifiableCollection(
            BAKED_MODEL_MESHES.asMap()
                .getOrDefault(model, Collections.emptySet()));
    }

    public static Multimap<MaterialParent, MaterialInstance> getMaterialInstances(ModelResourceLocation model)
    {
        // This SHOULD be unmodifiable, but just in case...
        return Multimaps.unmodifiableMultimap(
            MODEL_MATERIAL_INSTANCES.getOrDefault(model,
                Multimaps.forMap(Map.of())));
    }

    public static void rebakeModels(ModelManagerAccessor manager, ProfilerFiller reloadProfiler)
    {
        try
        {
            var bakery = manager.getModelBakery();

            reloadProfiler.startTick();
            reloadProfiler.push("archetype_discovery");
            var archetypeSet = new HashSet<ModelResourceLocation>();
            var modelArchetypes = new HashMap<ModelResourceLocation, BiMap<ModelResourceLocation, UnbakedModel>>();
            for (var pair : manager.getBakedRegistry().entrySet())
            {
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("Discovering archetypes of model {} ", pair.getKey());

                reloadProfiler.push(pair.getKey().toString());
                var archetypes = ArchetypeBaker.getArchetypes(pair.getKey(), manager, bakery);
                modelArchetypes.put(pair.getKey(), archetypes);
                archetypeSet.addAll(archetypes.keySet());
                reloadProfiler.pop();
            }

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Computed {} different archetype models", archetypeSet.size());

            reloadProfiler.popPush("archetype_baking");
            var archetypeMeshes = HashMultimap.<ModelResourceLocation, ModelResourceLocation>create();
            var bakedComponents = HashMultimap.<ModelResourceLocation, ArchetypeComponent>create();
            for (var archetype : archetypeSet)
            {
                var model = bakery.getModel(archetype.id());
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("Baking archetype model {} of model type {}", archetype, model.getClass());

                var components = ArchetypeBaker.bakeArchetypeComponents(archetype, model)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                if (components.isEmpty())
                {
                    LOGGER.warn("Archetype model {} returned no archetype components?", archetype);
                    continue;
                }

                for (var component : components)
                {
                    bakedComponents.put(component.name(), component);
                    archetypeMeshes.put(archetype, component.name());
                }
            }

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Baked {} different archetype meshes", archetypeMeshes.size());

            reloadProfiler.popPush("archetype_association");

            var reverseMap = ((BlockModelShaperAccessor)manager.getBlockModelShaper())
                .getModelByStateCache()
                .entrySet()
                .stream()
                .collect(Multimaps.toMultimap(
                    it -> BlockModelShaper.stateToModelLocation(it.getKey()),
                    Entry::getKey,
                    HashMultimap::create));

            // For every model...
            var bakedModelMeshes = modelArchetypes.entrySet().stream()
                // For every archetype for this model...
                .flatMap(model -> model.getValue().keySet().stream()
                    .map(archetype -> Map.entry(model.getKey(), archetype)))
                // For every archetype mesh for this archetype...
                .flatMap(model -> archetypeMeshes.asMap().getOrDefault(model.getValue(), Set.of()).stream()
                    .map(mesh -> Map.entry(model.getKey(), mesh)))
                // For every component for this archetype mesh...
                .flatMap(model -> bakedComponents.asMap().getOrDefault(model.getValue(), Set.of()).stream()
                    .map(component -> Map.entry(model.getKey(), component)))
                // Where the component is visible
                .filter(it -> it.getValue().isVisible())
                // Create a map of model -> baked mesh
                .collect(Multimaps.flatteningToMultimap(
                    Map.Entry::getKey,
                    it -> getDisplayMesh(
                        modelArchetypes.get(it.getKey()),
                        reverseMap.asMap().getOrDefault(it.getKey(), Set.of()),
                        bakery,
                        it.getKey(),
                        it.getValue()),
                    HashMultimap::create));

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Associated {} unique models to {} display meshes ({} min {} avg {} max display meshes per model)",
                    bakedModelMeshes.keySet().size(),
                    bakedModelMeshes.size(),
                    bakedModelMeshes.asMap().values().stream().mapToInt(Collection::size).min().orElse(0),
                    bakedModelMeshes.asMap().values().stream().mapToInt(Collection::size).average().orElse(0),
                    bakedModelMeshes.asMap().values().stream().mapToInt(Collection::size).max().orElse(0));

            reloadProfiler.popPush("archetype_cache");
            BAKED_MODEL_MESHES = Multimaps.unmodifiableMultimap(bakedModelMeshes);

            reloadProfiler.popPush("material_instances");
            var modelMaterials = new HashMap<ModelResourceLocation, Multimap<MaterialParent, MaterialInstance>>();
            for (var pair : manager.getBakedRegistry().entrySet())
            {
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("Baking material instances of model {}", pair.getKey());

                var unbakedModel = ((ModelBakeryAccessor)bakery).getTopLevelModels()
                    .get(pair.getKey());

                modelMaterials.put(pair.getKey(),
                    MaterialBaker.getMaterialInstances(bakery, pair.getKey(), unbakedModel)
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

    private static Stream<DisplayableMesh> getDisplayMesh(
        BiMap<ModelResourceLocation, UnbakedModel> archetypes,
        Collection<BlockState> sourceBlockStates,
        ModelBakery bakery,
        ModelResourceLocation model,
        ArchetypeComponent component)
    {
        var unbakedModel = ((ModelBakeryAccessor)bakery).getTopLevelModels().get(model);
        switch (unbakedModel)
        {
            case null ->
            {
                LOGGER.error("Failed to get unbaked model {}", model);
                return Stream.of(
                    new DisplayableMesh(
                        component.name(),
                        component.bakedMesh(),
                        component.renderType(),
                        Transformation.identity(),
                        1));
            }
            case BlockModel block ->
            {
                LOGGER.trace("Model {} is its own archetype?", model);
                return Stream.of(
                    new DisplayableMesh(
                        component.name(),
                        component.bakedMesh(),
                        component.renderType(),
                        Transformation.identity(),
                        1));
            }
            case MultiPart multiPart ->
            {
                var modelToArchetype = archetypes.inverse();
                var accessor = (MultiPartAccessor)multiPart;
                return accessor.getSelectors()
                    .stream()
                    .filter(p -> {
                        var predicate = p.getPredicate(accessor.getDefinition());
                        return sourceBlockStates.stream().anyMatch(predicate);
                    })
                    .map(Selector::getVariant)
                    .map(MultiVariant::getVariants)
                    .flatMap(List::stream)
                    .filter(it -> variantMatchesArchetype(modelToArchetype, bakery, it, component))
                    .map(it -> new DisplayableMesh(
                        component.name(),
                        component.bakedMesh(),
                        component.renderType(),
                        it.getRotation(),
                        it.getWeight()));
            }
            case MultiVariant multiVariant ->
            {
                var modelToArchetype = archetypes.inverse();
                return multiVariant.getVariants()
                    .stream()
                    .filter(it -> variantMatchesArchetype(modelToArchetype, bakery, it, component))
                    .map(it -> new DisplayableMesh(
                        component.name(),
                        component.bakedMesh(),
                        component.renderType(),
                        it.getRotation(),
                        it.getWeight()));
            }
            default -> {
                LOGGER.error("Unknown unbaked model type {}", unbakedModel.getClass());
                return Stream.of();
            }
        }
    }

    private static boolean variantMatchesArchetype(
        BiMap<UnbakedModel, ModelResourceLocation> modelToArchetype,
        ModelBakery bakery,
        Variant variant,
        ArchetypeComponent component)
    {
        // TODO: as of 1.21 getModel can only return a BlockModel. This may change in the future...
        var variantModel = (BlockModel)bakery.getModel(variant.getModelLocation());
        while (variantModel.parent != null)
        {
            if (modelToArchetype.containsKey(variantModel))
                break;

            variantModel = variantModel.parent;
        }

        return modelToArchetype.get(variantModel).id().equals(component.name().id());
    }
}
