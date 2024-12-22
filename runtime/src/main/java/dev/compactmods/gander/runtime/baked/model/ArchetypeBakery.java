package dev.compactmods.gander.runtime.baked.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import dev.compactmods.gander.render.baked.model.DisplayableMesh;
import dev.compactmods.gander.render.baked.model.DisplayableMeshGroup;
import dev.compactmods.gander.render.baked.model.DisplayableMeshGroup.Mode;
import dev.compactmods.gander.render.baked.model.archetype.ArchetypeComponent;
import dev.compactmods.gander.runtime.baked.model.archetype.ArchetypeBaker;
import dev.compactmods.gander.runtime.mixin.accessor.MultiPartAccessor;
import dev.compactmods.gander.runtime.mixin.accessor.TransformationAccessor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.block.model.UnbakedBlockStateModel;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.util.profiling.ProfilerFiller;

import net.minecraft.world.level.block.state.BlockState;

import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ArchetypeBakery
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchetypeBakery.class);

    private final Map<ResourceLocation, UnbakedModel> _unbakedArchetypes;
    private final Multimap<ResourceLocation, ResourceLocation> _referencedArchetypes;
    private final Multimap<ModelResourceLocation, ResourceLocation> _blockStateArchetypes;
    private final Map<ModelResourceLocation, BlockStateModelLoader.LoadedModel> _blockStateModels;

    public ArchetypeBakery(
        Map<ResourceLocation, UnbakedModel> unbakedArchetypes,
        Multimap<ResourceLocation, ResourceLocation> referencedArchetypes,
        Multimap<ModelResourceLocation, ResourceLocation> blockStateArchetypes,
        Map<ModelResourceLocation, BlockStateModelLoader.LoadedModel> blockStateModels)
    {
        _unbakedArchetypes = unbakedArchetypes;
        _referencedArchetypes = referencedArchetypes;
        _blockStateArchetypes = blockStateArchetypes;
        _blockStateModels = blockStateModels;
    }

    public BakingResult bakeArchetypes(ProfilerFiller profiler)
    {
        profiler.push("archetype_baking");

        Multimap<ResourceLocation, ArchetypeComponent> bakedComponents = HashMultimap.create();
        for (var pair : _unbakedArchetypes.entrySet())
        {
            var archetype = pair.getKey();
            var model = pair.getValue();

            if (LOGGER.isTraceEnabled())
                LOGGER.trace("Baking archetype model {} of model type {}", archetype, model.getClass());

            profiler.push(archetype.toString());

            var components = bake(null, archetype)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

            profiler.pop();

            if (components.isEmpty())
            {
                LOGGER.warn("Archetype model {} returned no archetype components?", archetype);
                continue;
            }

            bakedComponents.putAll(pair.getKey(), components);
        }

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Baked {} different archetype components", bakedComponents.size());

        profiler.popPush("archetype_association");
        Map<ModelResourceLocation, DisplayableMeshGroup> bakedBlockStates =
            Maps.transformEntries(_blockStateModels,
                (key, value) -> getMeshGroup(key,
                    value.state(),
                    value.model(),
                    _blockStateArchetypes.get(key),
                    _referencedArchetypes::get,
                    bakedComponents::get));

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Associated {} block states to {} display meshes ({} min {} avg {} max display meshes per block state)",
                bakedBlockStates.keySet().size(),
                bakedBlockStates.size(),
                bakedBlockStates.values().stream().mapToLong(it -> it.allMeshes().count()).min().orElse(0),
                bakedBlockStates.values().stream().mapToLong(it -> it.allMeshes().count()).average().orElse(0),
                bakedBlockStates.values().stream().mapToLong(it -> it.allMeshes().count()).max().orElse(0));

        profiler.pop();
        return new BakingResult(bakedComponents, bakedBlockStates);
    }

    private Stream<ArchetypeComponent> bake(
        ModelResourceLocation parent,
        ResourceLocation location)
    {
        var model = _unbakedArchetypes.get(location);
        var provider = ArchetypeBaker.getProvider(model);
        if (provider == null)
            throw new IllegalStateException("Unknown model kind: " + model);

        var name = parent == null
            ? new ModelResourceLocation(location, "")
            : new ModelResourceLocation(parent.id(), parent.variant() + location + "/");

        return provider.bake(name, model, this::bake);
    }

    private DisplayableMeshGroup getMeshGroup(
        ModelResourceLocation name,
        BlockState state,
        UnbakedBlockStateModel model,
        Collection<ResourceLocation> componentReferences,
        Function<ResourceLocation, Collection<ResourceLocation>> getArchetypes,
        Function<ResourceLocation, Collection<ArchetypeComponent>> getComponent)
    {
        switch (model)
        {
            case null ->
            {
                LOGGER.error("Failed to get unbaked block state model {}", name);
                return componentReferences.stream()
                    .map(getComponent)
                    .flatMap(Collection::stream)
                    .map(component -> new DisplayableMesh(
                        component.name(),
                        component.bakedMesh(),
                        RenderType.solid(),
                        //getRenderType(component.renderType(), sourceBlockState),
                        new Matrix4f(),
                        1,
                        x -> Set.of()))
                    .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        l -> DisplayableMeshGroup.ofMeshes(Mode.All, 1, l)));
            }
            case MultiPart multiPart ->
            {
                return ((MultiPartAccessor)multiPart).getSelectors()
                    .stream()
                    .filter(it -> it.predicate().test(state))
                    .map(MultiPart.InstantiatedSelector::variant)
                    .map(it -> getMeshGroup(
                        name,
                        state,
                        it,
                        componentReferences,
                        getArchetypes,
                        getComponent))
                    .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        g -> DisplayableMeshGroup.ofGroups(Mode.All, 1, g)));
            }
            case MultiVariant multiVariant ->
            {
                return multiVariant.variants()
                    .stream()
                    .map(it -> getVariantGroup(
                        name,
                        state,
                        it,
                        componentReferences,
                        getArchetypes,
                        getComponent))
                    .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        x -> DisplayableMeshGroup.ofGroups(Mode.Weighted, 1, x)));
            }
            default -> {
                LOGGER.error("Unknown unbaked block state model type {}", model.getClass());
                return null;
            }
        }
    }

    private DisplayableMeshGroup getVariantGroup(
        ModelResourceLocation model,
        BlockState state,
        Variant variant,
        Collection<ResourceLocation> componentReferences,
        Function<ResourceLocation, Collection<ResourceLocation>> getArchetypes,
        Function<ResourceLocation, Collection<ArchetypeComponent>> getComponent)
    {
        return getArchetypes.apply(variant.modelLocation())
            .stream()
            .map(getComponent)
            .flatMap(Collection::stream)
            .map(it -> new DisplayableMesh(
                it.name(),
                it.bakedMesh(),
                //it.renderType(),
                RenderType.solid(),
                ((TransformationAccessor)(Object)variant.getRotation()).gander$matrix(),
                1,
                x -> Set.of()))
            .collect(Collectors.collectingAndThen(
                Collectors.toList(),
                l -> DisplayableMeshGroup.ofMeshes(Mode.All, variant.weight(), l)));
    }

    public record BakingResult(
        Multimap<ResourceLocation, ArchetypeComponent> bakedArchetypes,
        Map<ModelResourceLocation, DisplayableMeshGroup> bakedBlockStates)
    { }
}
