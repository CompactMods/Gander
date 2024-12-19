package dev.compactmods.gander.runtime.baked.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import dev.compactmods.gander.render.baked.model.DisplayableMeshGroup;
import dev.compactmods.gander.render.baked.model.archetype.ArchetypeComponent;
import dev.compactmods.gander.runtime.baked.model.archetype.ArchetypeBaker;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ArchetypeBakery
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchetypeBakery.class);

    private final Multimap<ResourceLocation, ResourceLocation> _referencedArchetypes;
    private final Map<ResourceLocation, UnbakedModel> _unbakedArchetypes;

    public ArchetypeBakery(
        Map<ResourceLocation, UnbakedModel> unbakedArchetypes,
        Multimap<ResourceLocation, ResourceLocation> referencedArchetypes)
    {
        _referencedArchetypes = referencedArchetypes;
        _unbakedArchetypes = unbakedArchetypes;
    }

    public BakingResult bakeArchetypes()
    {
        Multimap<ResourceLocation, ArchetypeComponent> bakedComponents = HashMultimap.create();

        for (var pair : _unbakedArchetypes.entrySet())
        {
            var archetype = pair.getKey();
            var model = pair.getValue();

            if (LOGGER.isTraceEnabled())
                LOGGER.trace("Baking archetype model {} of model type {}", archetype, model.getClass());

            var components = bake(null, archetype)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

            if (components.isEmpty())
            {
                LOGGER.warn("Archetype model {} returned no archetype components?", archetype);
                continue;
            }

            bakedComponents.putAll(pair.getKey(), components);
        }

        return new BakingResult(bakedComponents, null);
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

    public static record BakingResult(
        Multimap<ResourceLocation, ArchetypeComponent> bakedArchetypes,
        Multimap<ResourceLocation, DisplayableMeshGroup> modelMeshes)
    { }
}
