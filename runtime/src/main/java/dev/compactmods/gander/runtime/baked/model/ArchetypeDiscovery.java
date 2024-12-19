package dev.compactmods.gander.runtime.baked.model;

import com.google.common.collect.BiMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;

import com.google.common.collect.Multimap;

import dev.compactmods.gander.runtime.mixin.accessor.BlockModelAccessor;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Discovers archetypes as part of model rebaking.
 */
public final class ArchetypeDiscovery
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchetypeDiscovery.class);

    private final Map<ResourceLocation, UnbakedModel> _referencedModels;
    private final UnbakedModel _missingModel;

    private final Map<ResourceLocation, UnbakedModel> _archetypes = new HashMap<>();
    private final Multimap<ResourceLocation, ResourceLocation> _referencedArchetypes = HashMultimap.create();

    public ArchetypeDiscovery(Map<ResourceLocation, UnbakedModel> referencedModels, UnbakedModel missingModel)
    {
        _referencedModels = referencedModels;
        _missingModel = missingModel;
    }

    public Map<ResourceLocation, UnbakedModel> getArchetypes()
    {
        return _archetypes;
    }

    public Multimap<ResourceLocation, ResourceLocation> getReferencedArchetypes()
    {
        return _referencedArchetypes;
    }

    public void discoverArchetypes(ProfilerFiller profiler)
    {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Beginning archetype discovery for {} models", _referencedModels.size());

        profiler.push("archetype_discovery");
        for (var pair : _referencedModels.entrySet())
        {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("Discovering archetypes of model {}", pair.getKey());

            profiler.push(pair.getKey().toString());
            var discovered = getArchetypes(pair.getKey(), pair.getValue());
            profiler.pop();

            if (LOGGER.isTraceEnabled())
                LOGGER.trace("Discovered {} archetypes for model {}", discovered.size(), pair.getKey());

            _referencedArchetypes.putAll(pair.getKey(), discovered.keySet());
            _archetypes.putAll(discovered);
        }
        profiler.pop();

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Computed {} different archetype models", _archetypes.size());
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

        var resolver = new ResolvableModel.Resolver()
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
