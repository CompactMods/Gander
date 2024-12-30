package dev.compactmods.gander.runtime.baked;

import com.google.common.collect.BiMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Multimap;

import com.mojang.datafixers.util.Either;

import dev.compactmods.gander.render.baked.model.DisplayableMesh;
import dev.compactmods.gander.render.baked.model.material.MaterialInstance;
import dev.compactmods.gander.render.baked.model.material.MaterialParent;
import dev.compactmods.gander.runtime.mixin.accessor.BlockModelAccessor;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.BlockStateModelLoader.LoadedModels;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvableModel.Resolver;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Discovers archetypes as part of model rebaking.
 */
public final class ArchetypeDiscovery
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchetypeDiscovery.class);

    private final Map<ResourceLocation, UnbakedModel> _referencedModels;
    private final UnbakedModel _missingModel;
    private final LoadedModels _blockStateModels;

    private final Map<ResourceLocation, UnbakedModel> _archetypes = new HashMap<>();
    private final Multimap<ResourceLocation, ResourceLocation> _referencedArchetypes = HashMultimap.create();
    private final Multimap<ModelResourceLocation, ResourceLocation> _blockStateArchetypes = HashMultimap.create();

    public ArchetypeDiscovery(
        Map<ResourceLocation, UnbakedModel> referencedModels,
        UnbakedModel missingModel,
        LoadedModels blockStateModels)
    {
        _referencedModels = referencedModels;
        _missingModel = missingModel;
        _blockStateModels = blockStateModels;
    }

    public Map<ResourceLocation, UnbakedModel> getArchetypes()
    {
        return _archetypes;
    }

    public Multimap<ResourceLocation, ResourceLocation> getReferencedArchetypes()
    {
        return _referencedArchetypes;
    }

    public Multimap<ModelResourceLocation, ResourceLocation> getBlockStateArchetypes()
    {
        return _blockStateArchetypes;
    }

    public void discoverArchetypes(ProfilerFiller profiler)
    {
        profiler.push("archetype_discovery");

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Beginning archetype discovery for {} models", _referencedModels.size());

        for (var pair : _referencedModels.entrySet())
        {
            profiler.push(pair.getKey().toString());
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("Discovering archetypes of model {}", pair.getKey());

            var discovered = getArchetypes(pair.getKey(), pair.getValue());

            if (LOGGER.isTraceEnabled())
                LOGGER.trace("Discovered {} archetypes for model {}", discovered.size(), pair.getKey());

            _referencedArchetypes.putAll(pair.getKey(), discovered.keySet());
            _archetypes.putAll(discovered);
            profiler.pop();
        }

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Computed {} different archetype models", _archetypes.size());

        profiler.popPush("blockstate_archetype_mapping");

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Beginning archetype discovery for {} blockstates", _blockStateModels.models().size());

        for (var pair : _blockStateModels.models().entrySet())
        {
            profiler.push(pair.getKey().toString());
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("Discovering archetypes of blockstate {}", pair.getValue().state());

            var discovered = getBlockStateArchetypes(pair.getValue().model());

            if (LOGGER.isTraceEnabled())
                LOGGER.trace("Discovered {} archetypes of blockstate {}", discovered.size(), pair.getValue().state());

            _blockStateArchetypes.putAll(pair.getKey(), discovered);
            profiler.pop();
        }

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Computed {} different blockstate archetype models", _blockStateArchetypes.size());

        profiler.pop();
    }

    private Set<ResourceLocation> getBlockStateArchetypes(ResolvableModel model)
    {
        var resolver = new Resolver()
        {
            final Builder<ResourceLocation> result
                = ImmutableSet.builder();
            final Set<ResourceLocation> visited
                = new HashSet<>();
            final Set<ResourceLocation> known
                = new HashSet<>();

            @Override
            public UnbakedModel resolve(ResourceLocation rl)
            {
                var dep = _referencedModels.getOrDefault(rl, _missingModel);

                if (dep != _missingModel)
                {
                    if (hasGeometry(dep))
                    {
                        if (known.add(rl))
                            result.add(rl);
                    }
                    else if (visited.add(rl))
                    {
                        dep.resolveDependencies(this);
                    }
                }

                return dep;
            }
        };

        model.resolveDependencies(resolver);

        return resolver.result.build();
    }

    private BiMap<ResourceLocation, UnbakedModel> getArchetypes(
        ResourceLocation location,
        UnbakedModel model)
    {
        // If the model itself has geometry, it is its own archetype.
        if (hasGeometry(model))
        {
            return ImmutableBiMap.of(location, model);
        }

        var resolver = new Resolver()
        {
            final ImmutableBiMap.Builder<ResourceLocation, UnbakedModel> result
                = ImmutableBiMap.builder();
            final Set<ResourceLocation> visited
                = new HashSet<>();
            final Set<ResourceLocation> known
                = new HashSet<>();

            @Override
            public UnbakedModel resolve(ResourceLocation rl)
            {
                var dep = _referencedModels.getOrDefault(rl, _missingModel);

                if (dep != _missingModel)
                {
                    if (hasGeometry(dep))
                    {
                        if (known.add(rl))
                            result.put(rl, dep);
                    }
                    else if (visited.add(rl))
                    {
                        dep.resolveDependencies(this);
                    }
                }

                return dep;
            }
        };

        model.resolveDependencies(resolver);

        return resolver.result.build();
    }

    // TODO: support custom geometry here
    private static boolean hasGeometry(UnbakedModel model)
    {
        // If the child model has its own geometry, it overrides any
        // parents.
        if (model instanceof BlockModel blockModel)
        {
            var modelAccessor = (BlockModelAccessor)blockModel;
            return !modelAccessor.getOwnElements().isEmpty();
        }

        return false;
    }
}
